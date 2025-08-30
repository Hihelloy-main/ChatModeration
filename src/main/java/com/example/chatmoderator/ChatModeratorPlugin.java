package com.example.chatmoderator;

import com.cjcrafter.foliascheduler.FoliaCompatibility;
import com.cjcrafter.foliascheduler.ServerImplementation;
import com.example.chatmoderator.config.ConfigManager;
import com.example.chatmoderator.listeners.ChatListener;
import com.example.chatmoderator.services.ModerationService;
import com.example.chatmoderator.commands.ChatModCommand;
import com.example.chatmoderator.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class ChatModeratorPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private ModerationService moderationService;
    private SchedulerUtil schedulerUtil;
    private ChatListener chatListener;

    public static ChatModeratorPlugin plugin;
    public static ServerImplementation scheduler;
    public static Logger log;

    @Override
    public void onEnable() {
        plugin = this;
        log = getLogger();

        // Folia scheduler setup
        scheduler = new FoliaCompatibility(this).getServerImplementation();

        // Initialize scheduler utility
        schedulerUtil = new SchedulerUtil(this);

        // Initialize configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize moderation service
        moderationService = new ModerationService(this);

        // Initialize ChatListener (pass plugin to constructor)
        chatListener = new ChatListener(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(chatListener, this);

        // Register commands with the listener passed
        ChatModCommand chatModCommand = new ChatModCommand(this, chatListener);
        getCommand("chatmod").setExecutor(chatModCommand);
        getCommand("chatmod").setTabCompleter(chatModCommand);

        log.info("ChatModerator plugin has been enabled!");

        // Check if OpenAI API key is configured
        String apiKey = configManager.getOpenAIApiKey();
        if (apiKey == null || apiKey.equals("your-openai-api-key-here")) {
            log.warning("OpenAI API key not configured! Please set it in config.yml");
            log.warning("AI moderation will be disabled until API key is provided.");
        }
    }

    @Override
    public void onDisable() {
        log.info("ChatModerator plugin has been disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ModerationService getModerationService() {
        return moderationService;
    }

    public SchedulerUtil getSchedulerUtil() {
        return schedulerUtil;
    }

    public void reloadPluginConfig() {
        configManager.reloadConfig();
        moderationService.updateConfiguration();
        Bukkit.getLogger().info("ChatModerator reloaded!");
    }

    public ChatListener getChatListener() {
        return chatListener;
    }
}
