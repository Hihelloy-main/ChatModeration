package com.Hihelloy.chatmoderator.config;

import com.Hihelloy.chatmoderator.ChatModeratorPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ConfigManager {

    private final ChatModeratorPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(ChatModeratorPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();  // Saves the default configuration file if it doesn't exist
        config = plugin.getConfig();
        applyNewConfigOptions();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        applyNewConfigOptions();
    }

    // This method applies new configuration options that might have been added in a newer version of the plugin
    public void applyNewConfigOptions() {
        // Ensure that new or updated config values are added/initialized
        if (!config.contains("ai.preferred-provider")) {
            config.set("ai.preferred-provider", "openai");  // Default value if not present
        }
        if (!config.contains("moderation.use-ai-moderation")) {
            config.set("moderation.use-ai-moderation", true);  // Default value if not present
        }
        if (!config.contains("moderation.use-word-filter")) {
            config.set("moderation.use-word-filter", true);  // Default value if not present
        }
        if (!config.contains("actions.block-message")) {
            config.set("actions.block-message", true);  // Default value if not present
        }
        if (!config.contains("actions.warn-player")) {
            config.set("actions.warn-player", true);  // Default value if not present
        }
        if (!config.contains("actions.notify-admins")) {
            config.set("actions.notify-admins", true);  // Default value if not present
        }
        if (!config.contains("actions.log-violations")) {
            config.set("actions.log-violations", true);  // Default value if not present
        }
        if (!config.contains("debug.enabled")) {
            config.set("debug.enabled", false);  // Default value if not present
        }
        if (!config.contains("debug.log-all-messages")) {
            config.set("debug.log-all-messages", false);  // Default value if not present
        }
        if (!config.contains("moderation.mute-duration-seconds")) {
            config.set("moderation.mute-duration-seconds", 600);  // Default value if not present
        }
        if (!config.contains("messages.message-blocked")) {
            config.set("messages.message-blocked", "&cYour message was blocked by the chat filter.");  // Default value
        }
        if (!config.contains("messages.violation-warning")) {
            config.set("messages.violation-warning", "&eYour message contains inappropriate content. You have been muted, ask an admin for an unmute.");  // Default value
        }
        if (!config.contains("messages.admin-notification")) {
            config.set("messages.admin-notification", "&6[ChatMod] &c{player} &7tried to send: &f{message}");  // Default value
        }
        if (!config.contains("messages.plugin-reloaded")) {
            config.set("messages.plugin-reloaded", "&aChat Moderator configuration reloaded!");  // Default value
        }
        if (!config.contains("messages.plugin-enabled")) {
            config.set("messages.plugin-enabled", "&aChat moderation enabled!");  // Default value
        }
        if (!config.contains("messages.plugin-disabled")) {
            config.set("messages.plugin-disabled", "&cChat moderation disabled!");  // Default value
        }

        // Save the updated config file
        plugin.saveConfig();
    }

    // AI Configuration
    public String getOpenAIApiKey() {
        return config.getString("openai.api-key", "");
    }

    public String getOpenAIModel() {
        return config.getString("openai.model", "text-moderation-latest");
    }

    public String getGeminiApiKey() {
        return config.getString("gemini.api-key", "");
    }

    public String getGeminiModel() {
        return config.getString("gemini.model", "gemini-1.5-flash");
    }

    public String getPreferredAIProvider() {
        return config.getString("ai.preferred-provider", "openai");
    }

    // Moderation Settings
    public boolean isModerationEnabled() {
        return config.getBoolean("moderation.enabled", true);
    }

    public boolean isAIModerationEnabled() {
        return config.getBoolean("moderation.use-ai-moderation", true);
    }

    public boolean isWordFilterEnabled() {
        return config.getBoolean("moderation.use-word-filter", true);
    }

    public List<String> getBlockedWords() {
        return config.getStringList("moderation.blocked-words");
    }

    public Map<String, Double> getModerationThresholds() {
        Map<String, Double> thresholds = new HashMap<>();
        if (config.isConfigurationSection("moderation.thresholds")) {
            for (String key : config.getConfigurationSection("moderation.thresholds").getKeys(false)) {
                thresholds.put(key, config.getDouble("moderation.thresholds." + key));
            }
        }
        return thresholds;
    }

    // Action Settings
    public boolean shouldBlockMessage() {
        return config.getBoolean("actions.block-message", true);
    }

    public boolean shouldWarnPlayer() {
        return config.getBoolean("actions.warn-player", true);
    }

    public boolean shouldNotifyAdmins() {
        return config.getBoolean("actions.notify-admins", true);
    }

    public boolean shouldLogViolations() {
        return config.getBoolean("actions.log-violations", true);
    }

    // Messages
    public String getMessageBlocked() {
        return config.getString("messages.message-blocked", "&cYour message was blocked by the chat filter.");
    }

    public String getViolationWarning() {
        return config.getString("messages.violation-warning", "&eYour message contains inappropriate content. You have been muted, ask an admin for an unmute.");
    }

    public String getAdminNotification() {
        return config.getString("messages.admin-notification", "&6[ChatMod] &c{player} &7tried to send: &f{message}");
    }

    public String getPluginReloaded() {
        return config.getString("messages.plugin-reloaded", "&aChat Moderator configuration reloaded!");
    }

    public String getPluginEnabled() {
        return config.getString("messages.plugin-enabled", "&aChat moderation enabled!");
    }

    public String getPluginDisabled() {
        return config.getString("messages.plugin-disabled", "&cChat moderation disabled!");
    }

    // Debug Settings
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public boolean shouldLogAllMessages() {
        return config.getBoolean("debug.log-all-messages", false);
    }

    public int getMuteDurationSeconds() {
        return config.getInt("moderation.mute-duration-seconds", 600);
    }
}
