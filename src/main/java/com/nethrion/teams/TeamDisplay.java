package com.nethrion.teams;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public final class TeamDisplay {
    private final JavaPlugin plugin;
    private final TeamManager manager;
    private final boolean enabled;
    private final String marker;
    private BukkitTask refreshTask;

    public TeamDisplay(JavaPlugin plugin, TeamManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.enabled = plugin.getConfig().getBoolean("settings.tablist-tag-enabled", true);
        this.marker = ChatColor.DARK_GRAY + "┃ " + ChatColor.GRAY;
    }

    public void start() {
        if (!enabled) return;
        refreshTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::refreshAll, 20L, 40L);
    }

    public void stop() {
        if (refreshTask != null) refreshTask.cancel();
    }

    public void refreshAll() {
        for (Player player : plugin.getServer().getOnlinePlayers()) refresh(player);
    }

    public void refresh(Player player) {
        if (!enabled || player == null) return;
        String current = player.getPlayerListName();
        String base = stripTeamTag(current);
        String team = manager.teamTag(player);
        player.setPlayerListName(team.isEmpty() ? base : base + marker + team);
    }

    public void refreshIfOnline(UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null) refresh(player);
    }

    private String stripTeamTag(String current) {
        if (current == null) return "";
        int index = current.lastIndexOf(marker);
        return index >= 0 ? current.substring(0, index) : current;
    }
}
