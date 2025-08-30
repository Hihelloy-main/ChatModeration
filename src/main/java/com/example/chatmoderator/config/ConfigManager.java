package com.example.chatmoderator.config;

import com.example.chatmoderator.ChatModeratorPlugin;
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
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }
    
    // AI Configuration
    public String getOpenAIApiKey() {
        return config.getString("openai.api-key", "");
    }
    
    public String getOpenAIModel() {
        return config.getString("openai.model", "text-moderation-latest");
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