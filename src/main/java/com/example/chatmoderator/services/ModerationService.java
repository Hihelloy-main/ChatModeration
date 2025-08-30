package com.example.chatmoderator.services;

import com.example.chatmoderator.ChatModeratorPlugin;
import com.example.chatmoderator.config.ConfigManager;
import com.example.chatmoderator.utils.SchedulerUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ModerationService {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;
    private final OkHttpClient httpClient;
    private final Gson gson;

    private static final String OPENAI_MODERATION_URL = "https://api.openai.com/v1/moderations";

    // Rate limiter: max requests per minute
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    public ModerationService(ChatModeratorPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();
        this.gson = new Gson();
    }

    /**
     * Post-send moderation: message is already sent to players, then AI checks happen asynchronously
     */
    public void moderateAfterSend(Player sender, String message) {
        // Run asynchronously to not block chat
        SchedulerUtil.runAsync(() -> {
            try {
                // 1. Word filter check
                ModerationResult wordFilterResult = checkWordFilter(message);
                if (wordFilterResult.isBlocked()) {
                    hideBlockedMessage(sender, message, wordFilterResult);
                    return;
                }

                // 2. AI moderation check
                if (configManager.isAIModerationEnabled()) {
                    ModerationResult aiResult = checkAIModerationWithRetries(message, 0);
                    if (aiResult.isBlocked()) {
                        hideBlockedMessage(sender, message, aiResult);
                    }
                }

            } catch (Exception e) {
                plugin.getLogger().severe("Error during post-send moderation: " + e.getMessage());
                if (configManager.isDebugEnabled()) e.printStackTrace();
            }
        });
    }

    /**
     * Method used for /chatmod aitest to test moderation synchronously
     */
    public CompletableFuture<ModerationResult> moderateMessageTest(String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ModerationResult wordResult = checkWordFilter(message);
                if (wordResult.isBlocked()) return wordResult;
                if (configManager.isAIModerationEnabled()) {
                    return checkAIModerationWithRetries(message, 0);
                }
                return new ModerationResult(false, "No violations detected", ModerationResult.ViolationType.NONE);
            } catch (Exception e) {
                plugin.getLogger().severe("Error during test moderation: " + e.getMessage());
                return new ModerationResult(false, "Moderation error", ModerationResult.ViolationType.ERROR);
            }
        });
    }

    public void hideBlockedMessage(Player sender, String message, ModerationResult result) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission("chatmoderator.bypass")) {
                player.sendMessage("[MODERATION] " + sender.getName() + " tried to send: " + message +
                        " | Reason: " + result.getReason());
            }
        }
    }

    /**
     * Word filter check (public so commands can access it)
     */
    public ModerationResult checkWordFilter(String message) {
        List<String> blockedWords = configManager.getBlockedWords();
        String lowerMessage = message.toLowerCase();

        for (String word : blockedWords) {
            if (lowerMessage.contains(word.toLowerCase())) {
                return new ModerationResult(
                        true,
                        "Contains blocked word: " + word,
                        ModerationResult.ViolationType.WORD_FILTER
                );
            }
        }
        return new ModerationResult(false, "No blocked words found", ModerationResult.ViolationType.NONE);
    }

    /**
     * AI moderation check (public so commands can access it)
     */
    public ModerationResult checkAIModerationWithRetries(String message, int retryCount) throws Exception {
        String apiKey = configManager.getOpenAIApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your-openai-api-key-here")) {
            return new ModerationResult(false, "AI moderation not configured", ModerationResult.ViolationType.NONE);
        }

        RateLimiter limiter = rateLimiters.computeIfAbsent(apiKey, k -> new RateLimiter(MAX_REQUESTS_PER_MINUTE));
        if (!limiter.tryAcquire()) {
            return new ModerationResult(false, "Rate limit exceeded", ModerationResult.ViolationType.ERROR);
        }

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", configManager.getOpenAIModel());
        requestBody.addProperty("input", message);

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder()
                .url(OPENAI_MODERATION_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.code() == 429) {
                if (retryCount < 5) {
                    long backoffMillis = (long) Math.pow(2, retryCount) * 500;
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().info("Rate limited by AI moderation API. Retrying in " + backoffMillis + "ms");
                    }
                    Thread.sleep(backoffMillis);
                    return checkAIModerationWithRetries(message, retryCount + 1);
                } else {
                    return new ModerationResult(false, "AI moderation rate limit exceeded", ModerationResult.ViolationType.ERROR);
                }
            }

            if (!response.isSuccessful()) {
                plugin.getLogger().warning("AI moderation API failed: " + response.code() + " " + response.message());
                return new ModerationResult(false, "AI moderation request failed", ModerationResult.ViolationType.ERROR);
            }

            String responseBody = response.body().string();
            JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonObject result = jsonResponse.getAsJsonArray("results").get(0).getAsJsonObject();
            boolean flagged = result.get("flagged").getAsBoolean();

            if (!flagged) return new ModerationResult(false, "AI moderation passed", ModerationResult.ViolationType.NONE);

            JsonObject categories = result.getAsJsonObject("categories");
            JsonObject categoryScores = result.getAsJsonObject("category_scores");
            Map<String, Double> thresholds = configManager.getModerationThresholds();

            for (Map.Entry<String, JsonElement> entry : categories.entrySet()) {
                String category = entry.getKey();
                boolean categoryFlagged = entry.getValue().getAsBoolean();

                if (categoryFlagged) {
                    double score = categoryScores.get(category).getAsDouble();
                    double threshold = thresholds.getOrDefault(category, 0.5);
                    if (score >= threshold) {
                        return new ModerationResult(
                                true,
                                "AI detected violation: " + category + " (score: " + score + ")",
                                ModerationResult.ViolationType.AI_DETECTED
                        );
                    }
                }
            }

            return new ModerationResult(false, "AI moderation passed", ModerationResult.ViolationType.NONE);

        } catch (IOException e) {
            plugin.getLogger().severe("Error parsing AI moderation response: " + e.getMessage());
            if (configManager.isDebugEnabled()) e.printStackTrace();
            return new ModerationResult(false, "AI moderation error", ModerationResult.ViolationType.ERROR);
        }
    }

    public void updateConfiguration() {
        if (configManager.isDebugEnabled()) {
            plugin.getLogger().info("Moderation service configuration updated");
        }
    }

    private static class RateLimiter {
        private final int maxRequestsPerMinute;
        private int availableTokens;
        private long lastRefillTimestamp;

        public RateLimiter(int maxRequestsPerMinute) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
            this.availableTokens = maxRequestsPerMinute;
            this.lastRefillTimestamp = Instant.now().toEpochMilli();
        }

        public synchronized boolean tryAcquire() {
            refill();
            if (availableTokens > 0) {
                availableTokens--;
                return true;
            }
            return false;
        }

        private void refill() {
            long now = Instant.now().toEpochMilli();
            long millisSinceLast = now - lastRefillTimestamp;
            long tokensToAdd = millisSinceLast * maxRequestsPerMinute / 60000;
            if (tokensToAdd > 0) {
                availableTokens = Math.min(maxRequestsPerMinute, availableTokens + (int) tokensToAdd);
                lastRefillTimestamp = now;
            }
        }
    }

    public static class ModerationResult {
        private final boolean blocked;
        private final String reason;
        private final ViolationType type;

        public ModerationResult(boolean blocked, String reason, ViolationType type) {
            this.blocked = blocked;
            this.reason = reason;
            this.type = type;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getReason() {
            return reason;
        }

        public ViolationType getType() {
            return type;
        }

        public enum ViolationType {
            NONE, WORD_FILTER, AI_DETECTED, ERROR
        }
    }
}
