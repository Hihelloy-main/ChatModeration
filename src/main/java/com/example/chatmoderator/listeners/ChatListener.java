package com.example.chatmoderator.listeners;

import com.example.chatmoderator.ChatModeratorPlugin;
import com.example.chatmoderator.config.ConfigManager;
import com.example.chatmoderator.services.ModerationService;
import com.example.chatmoderator.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;
    private final ModerationService moderationService;

    // Map to store muted players and their unmute timestamp
    private final Map<Player, Long> mutedPlayers = new ConcurrentHashMap<>();

    public ChatListener(ChatModeratorPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.moderationService = plugin.getModerationService();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // Check if player is muted
        if (isMuted(player)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You are temporarily muted!");
            return;
        }

        // Word filter check (immediate)
        if (configManager.isWordFilterEnabled()) {
            for (String word : configManager.getBlockedWords()) {
                if (message.toLowerCase().contains(word.toLowerCase())) {
                    event.setCancelled(true);
                    blockMessage(player, message, "Contains blocked word: " + word);
                    return;
                }
            }
        }

        // Bypass permission: allow message to go through instantly
        if (player.hasPermission("chatmoderator.bypass")) {
            return;
        }

        // Post-send AI moderation
        if (configManager.isAIModerationEnabled()) {
            // Message already sent, now check asynchronously
            moderationService.moderateAfterSend(player, message);
        }
    }

    private void blockMessage(Player player, String message, String reason) {
        // Cancel sending for everyone
        SchedulerUtil.runGlobal(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.isOp() || p.hasPermission("chatmoderator.bypass")) {
                    p.sendMessage(ChatColor.RED + "[MODERATION] " + player.getName() + " tried to send: " + message + " | Reason: " + reason);
                }
            }
        });

        // Mute player
        int muteDurationSeconds = configManager.getMuteDurationSeconds();
        mutedPlayers.put(player, System.currentTimeMillis() + muteDurationSeconds * 1000L);

        // Warn the player
        if (configManager.shouldWarnPlayer()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', configManager.getViolationWarning()));
        }

        // Log violation
        if (configManager.shouldLogViolations()) {
            plugin.getLogger().warning("Chat violation by " + player.getName() + " | Reason: " + reason + " | Message: " + message);
        }
    }

    public boolean isMuted(Player player) {
        if (!mutedPlayers.containsKey(player)) return false;

        long unmuteTime = mutedPlayers.get(player);
        if (System.currentTimeMillis() >= unmuteTime) {
            mutedPlayers.remove(player);
            return false;
        }
        return true;
    }

    public void unmutePlayer(Player player) {
        if (mutedPlayers.containsKey(player)) {
            mutedPlayers.remove(player);
            player.sendMessage(ChatColor.GREEN + "You have been unmuted by an admin.");
        } else {
            player.sendMessage(ChatColor.RED + "You were not muted.");
        }
    }

    public Map<Player, Long> getMutedPlayers() {
        return mutedPlayers;
    }
}
