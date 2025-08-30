package com.example.chatmoderator.commands;

import com.example.chatmoderator.ChatModeratorPlugin;
import com.example.chatmoderator.config.ConfigManager;
import com.example.chatmoderator.listeners.ChatListener;
import com.example.chatmoderator.services.ModerationService;
import com.example.chatmoderator.services.ModerationService.ModerationResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChatModCommand implements CommandExecutor, TabCompleter {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;
    private final ChatListener chatListener;
    private final ModerationService moderationService;

    public ChatModCommand(ChatModeratorPlugin plugin, ChatListener chatListener) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.chatListener = chatListener;
        this.moderationService = plugin.getModerationService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
            case "status":
            case "toggle":
            case "add-word":
            case "remove-word":
                if (!sender.hasPermission("chatmoderator.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                break;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;

            case "status":
                handleStatus(sender);
                break;

            case "toggle":
                handleToggle(sender);
                break;

            case "add-word":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatmod add-word <word>");
                    return true;
                }
                handleAddWord(sender, args[1]);
                break;

            case "remove-word":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatmod remove-word <word>");
                    return true;
                }
                handleRemoveWord(sender, args[1]);
                break;

            case "unmute":
                if (!sender.hasPermission("chatmoderator.admin") &&
                        !sender.hasPermission("chatmoderator.command.unmute")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatmod unmute <player>");
                    return true;
                }
                handleUnmute(sender, args[1]);
                break;

            case "aitest":
                if (!sender.hasPermission("chatmoderator.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /chatmod aitest <message>");
                    return true;
                }
                handleAITest(sender, String.join(" ", args).replaceFirst("aitest ", ""));
                break;

            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Chat Moderator Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod reload " + ChatColor.WHITE + "- Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod status " + ChatColor.WHITE + "- Show plugin status");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod toggle " + ChatColor.WHITE + "- Toggle moderation on/off");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod add-word <word> " + ChatColor.WHITE + "- Add blocked word");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod remove-word <word> " + ChatColor.WHITE + "- Remove blocked word");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod unmute <player> " + ChatColor.WHITE + "- Unmute a muted player");
        sender.sendMessage(ChatColor.YELLOW + "/chatmod aitest <message> " + ChatColor.WHITE + "- Test AI moderation");
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.reloadPluginConfig();
            String message = ChatColor.translateAlternateColorCodes('&', configManager.getPluginReloaded());
            sender.sendMessage(message);
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Error reloading configuration: " + e.getMessage());
        }
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Chat Moderator Status ===");
        sender.sendMessage(ChatColor.YELLOW + "Moderation Enabled: " +
                (configManager.isModerationEnabled() ? ChatColor.GREEN + "Yes" : ChatColor.RED + "No"));
        sender.sendMessage(ChatColor.YELLOW + "AI Moderation: " +
                (configManager.isAIModerationEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Word Filter: " +
                (configManager.isWordFilterEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
        sender.sendMessage(ChatColor.YELLOW + "Blocked Words: " + ChatColor.WHITE + configManager.getBlockedWords().size());

        String apiKey = configManager.getOpenAIApiKey();
        boolean apiConfigured = apiKey != null && !apiKey.equals("your-openai-api-key-here");
        sender.sendMessage(ChatColor.YELLOW + "OpenAI API: " +
                (apiConfigured ? ChatColor.GREEN + "Configured" : ChatColor.RED + "Not Configured"));
    }

    private void handleToggle(CommandSender sender) {
        FileConfiguration config = plugin.getConfig();
        boolean currentState = configManager.isModerationEnabled();
        boolean newState = !currentState;

        config.set("moderation.enabled", newState);
        plugin.saveConfig();
        configManager.reloadConfig();

        String message = newState ? configManager.getPluginEnabled() : configManager.getPluginDisabled();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private void handleAddWord(CommandSender sender, String word) {
        FileConfiguration config = plugin.getConfig();
        List<String> blockedWords = new ArrayList<>(config.getStringList("moderation.blocked-words"));

        if (blockedWords.contains(word.toLowerCase())) {
            sender.sendMessage(ChatColor.RED + "Word '" + word + "' is already in the blocked list.");
            return;
        }

        blockedWords.add(word.toLowerCase());
        config.set("moderation.blocked-words", blockedWords);
        plugin.saveConfig();
        configManager.reloadConfig();

        sender.sendMessage(ChatColor.GREEN + "Added '" + word + "' to blocked words list.");
    }

    private void handleRemoveWord(CommandSender sender, String word) {
        FileConfiguration config = plugin.getConfig();
        List<String> blockedWords = new ArrayList<>(config.getStringList("moderation.blocked-words"));

        if (!blockedWords.contains(word.toLowerCase())) {
            sender.sendMessage(ChatColor.RED + "Word '" + word + "' is not in the blocked list.");
            return;
        }

        blockedWords.remove(word.toLowerCase());
        config.set("moderation.blocked-words", blockedWords);
        plugin.saveConfig();
        configManager.reloadConfig();

        sender.sendMessage(ChatColor.GREEN + "Removed '" + word + "' from blocked words list.");
    }

    private void handleUnmute(CommandSender sender, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return;
        }

        if (chatListener.isMuted(target)) {
            chatListener.unmutePlayer(target);
            sender.sendMessage(ChatColor.GREEN + "Player " + target.getName() + " has been unmuted.");
            Bukkit.broadcast("Player " + target.getName() + " has been unmuted.", "chatmoderator.null");

        } else {
            sender.sendMessage(ChatColor.RED + "Player " + target.getName() + " is not muted.");
        }
    }

    private void handleAITest(CommandSender sender, String message) {
        // Run AI moderation asynchronously without blocking main thread
        moderationService.moderateMessageTest(message).thenAccept(result -> {
            sender.sendMessage(ChatColor.GOLD + "=== AI Moderation Test Result ===");
            sender.sendMessage(ChatColor.YELLOW + "Blocked: " + (result.isBlocked() ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
            sender.sendMessage(ChatColor.YELLOW + "Reason: " + result.getReason());
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String[] subcommands = {"reload", "status", "toggle", "add-word", "remove-word", "unmute", "aitest"};
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) completions.add(sub);
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("remove-word")) {
                for (String word : configManager.getBlockedWords()) {
                    if (word.startsWith(args[1].toLowerCase())) completions.add(word);
                }
            } else if (args[0].equalsIgnoreCase("unmute")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (chatListener.isMuted(p) && p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        }

        return completions;
    }
}
