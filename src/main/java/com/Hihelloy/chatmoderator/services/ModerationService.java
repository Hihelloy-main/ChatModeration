package com.Hihelloy.chatmoderator.services;

import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import com.Hihelloy.chatmoderator.config.ConfigManager;
import com.Hihelloy.chatmoderator.utils.ModerationResult;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ModerationService {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;
    private final Client geminiClient;

    public ModerationService(ChatModeratorPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        // Initialize Gemini client if AI provider is Gemini
        if ("gemini".equalsIgnoreCase(configManager.getPreferredAIProvider())) {
            String apiKey = configManager.getGeminiApiKey();
            if (apiKey != null && !apiKey.equals("your-gemini-api-key-here")) {
                this.geminiClient = new Client();
            } else {
                this.geminiClient = null; // disable AI moderation
            }
        } else {
            this.geminiClient = null; // AI provider not Gemini
        }
    }

    /**
     * Asynchronously checks a message for moderation
     */
    public CompletableFuture<ModerationResult> checkAIModerationAsync(String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (geminiClient != null) {
                    String prompt = "Classify this chat message for moderation.\n\n" +
                            "Message: \"" + message + "\"\n\n" +
                            "Respond with only ONE label: SAFE, HATE, SEXUAL, VIOLENCE, SELF_HARM.";

                    GenerateContentResponse response = geminiClient.models
                            .generateContent(configManager.getGeminiModel(), prompt, null);

                    if (response != null && response.text() != null) {
                        String result = response.text().trim().toUpperCase(Locale.ROOT);
                        switch (result) {
                            case "SEXUAL":
                                return ModerationResult.safe();
                            case "VIOLENCE":
                            case "SELF_HARM":
                                return ModerationResult.block("AI flagged: " + result);
                        }
                    }
                }
                // fallback to rule-based check
                return checkAIRules(message);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "AI moderation failed", e);
                return checkAIRules(message);
            }
        });
    }

    /**
     * Simple keyword-based moderation fallback
     */
    private ModerationResult checkAIRules(String message) {
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("kys") || lower.contains("nigger") || lower.contains("faggot") || lower.contains("negro") || lower.equalsIgnoreCase("Kill yourself") || lower.contains("gfys") || lower.contains("nga") || lower.contains("nigga") || lower.contains("ngr") || lower.contains("retard") || lower.contains("fag") || lower.contains("sped")) {
            return ModerationResult.block("Rule: violence/self-harm/hate-speech keyword/keyphrase detected");
        }
        if (lower.contains("sex") || lower.contains("rape") || lower.equalsIgnoreCase("I'm horny") || lower.contains("horny") || lower.contains("nude") || lower.contains("naked") || lower.equalsIgnoreCase("I'm gonna rape you") || lower.equalsIgnoreCase("I'm going to goon") || lower.contains("goon") || lower.contains("blowjob") || lower.equalsIgnoreCase("I'm gonna blow you") || lower.contains("cum") || lower.equalsIgnoreCase("I'm cumming") || lower.equalsIgnoreCase("fuck me in the ass") || lower.equalsIgnoreCase("goon me") || lower.equalsIgnoreCase("fiddle with me") || lower.equalsIgnoreCase("twirl my pubes") || lower.contains("pubes") || lower.equalsIgnoreCase("blow me")) {
            return ModerationResult.block("Rule: sexual content keyword/keyphrase detected");
        }
        return ModerationResult.safe();
    }

}
