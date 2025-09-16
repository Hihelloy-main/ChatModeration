package com.example.chatmoderator.listeners;

import com.example.chatmoderator.ChatModeratorPlugin;
import com.example.chatmoderator.config.ConfigManager;
import com.example.chatmoderator.services.ModerationService;
import com.example.chatmoderator.utils.ModerationResult;
import com.example.chatmoderator.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatListener implements Listener {

    private final ChatModeratorPlugin plugin;
    private final ConfigManager configManager;
    private final ModerationService moderationService;

    private final Map<Player, Long> mutedPlayers = new ConcurrentHashMap<>();

    public ChatListener(ChatModeratorPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.moderationService = plugin.getModerationService();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        if (configManager.isDebugEnabled() && configManager.shouldLogAllMessages()) {
            Bukkit.getLogger().info("[DEBUG] Chat message from " + event.getPlayer().getName()
                    + ": " + event.getMessage());
        }

        if (event.isCancelled()) {
            if (configManager.isDebugEnabled()) {
                Bukkit.getLogger().info("[DEBUG] Chat message from " + event.getPlayer().getName()
                        + " was blocked.");
            }
        }

        if (isMuted(event.getPlayer())) {
            if (configManager.isDebugEnabled()) {
                Bukkit.getLogger().info("[DEBUG] Muted player " + event.getPlayer().getName()
                        + " attempted to send a message.");
            }
        }

        if (configManager.isDebugEnabled()) {
            Bukkit.getLogger().info("[DEBUG] Current muted players: " + mutedPlayers.keySet());
        }
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
                    blockMessageAndBroadcast(player, message, "Contains blocked word: " + word);
                    return;
                }
            }
        }

        // Bypass permission
        if (player.hasPermission("chatmoderator.bypass")) {
            event.setCancelled(false);
            return;
        }

        // AI Moderation (async)
        if (configManager.isAIModerationEnabled()) {
            moderationService.checkAIModerationAsync(message)
                    .thenAccept(moderationResult -> {
                        if (moderationResult.isBlocked()) {
                            // Ensure cancellation and player notification run on main thread
                            SchedulerUtil.runGlobal(() -> {
                                event.setCancelled(true);
                                blockMessageAndBroadcast(player, message, moderationResult.getReason());
                            });
                        }
                    });
        }
    }

    // Prevent muted players from using /msg, /tell, /w, etc.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isMuted(player)) return;

        String msg = event.getMessage().toLowerCase();
        if (msg.startsWith("/msg ") || msg.startsWith("/tell ") || msg.startsWith("/w ") || msg.startsWith("/whisper ") || msg.startsWith("/pm ")) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You are muted and cannot send private messages.");
        }
    }

    // Block message, mute, and broadcast to all players
    private void blockMessageAndBroadcast(Player player, String message, String reason) {
        // Broadcast to all players
        String broadcastMsg = ChatColor.RED + "[MODERATION] " + player.getName()
                + " has been muted for: " + reason + "\n" +
                ChatColor.YELLOW + "Blocked message: " + message;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(broadcastMsg);
        }

        // Mute player
        int muteDurationSeconds = configManager.getMuteDurationSeconds();
        mutedPlayers.put(player, System.currentTimeMillis() + muteDurationSeconds * 1000L);

        // Warn player
        if (configManager.shouldWarnPlayer()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    configManager.getViolationWarning()));
        }

        // Log
        if (configManager.shouldLogViolations()) {
            plugin.getLogger().warning("Chat violation by " + player.getName()
                    + " | Reason: " + reason + " | Message: " + message);
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