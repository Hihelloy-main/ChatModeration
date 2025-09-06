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

        // Load configuration
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        // Initialize ModerationService (fully initialized)
        moderationService = new ModerationService(this, configManager);

        // Initialize ChatListener
        chatListener = new ChatListener(this);

        // Register event listeners
        getServer().getPluginManager().registerEvents(chatListener, this);

        // Register commands
        ChatModCommand chatModCommand = new ChatModCommand(this, chatListener);
        getCommand("chatmod").setExecutor(chatModCommand);
        getCommand("chatmod").setTabCompleter(chatModCommand);

        log.info("ChatModerator plugin has been enabled!");

        // Warn if AI keys missing
        checkAPIKeys();
    }

    @Override
    public void onDisable() {
        SchedulerUtil.shutdown();
        log.info("ChatModerator plugin has been disabled!");
    }

    private void checkAPIKeys() {
        String aiProvider = configManager.getPreferredAIProvider();
        String openaiApiKey = configManager.getOpenAIApiKey();
        String geminiApiKey = configManager.getGeminiApiKey();

        if ("openai".equalsIgnoreCase(aiProvider) && (openaiApiKey == null || openaiApiKey.equals("your-openai-api-key-here"))) {
            log.warning("OpenAI API key not configured! AI moderation will be disabled until key is provided.");
        } else if ("gemini".equalsIgnoreCase(aiProvider) && (geminiApiKey == null || geminiApiKey.equals("your-gemini-api-key-here"))) {
            log.warning("Gemini API key not configured! AI moderation will be disabled until key is provided.");
        }
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

    public ChatListener getChatListener() {
        return chatListener;
    }

    public void reloadPluginConfig() {
        configManager.reloadConfig();
        configManager.applyNewConfigOptions();
        Bukkit.getLogger().info("ChatModerator reloaded!");
    }
}
