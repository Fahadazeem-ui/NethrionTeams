package com.nethrion.teams;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class NethrionTeamsPlugin extends JavaPlugin {
    private TeamManager teamManager;
    private TeamDisplay teamDisplay;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        teamManager = new TeamManager(this);
        teamDisplay = new TeamDisplay(this, teamManager);

        PluginCommand teamCommand = getCommand("team");
        if (teamCommand == null) {
            getLogger().severe("/team command is missing from plugin.yml; disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        TeamCommand executor = new TeamCommand(teamManager, teamDisplay);
        teamCommand.setExecutor(executor);
        teamCommand.setTabCompleter(executor);

        getServer().getPluginManager().registerEvents(new TeamProtectionListener(teamManager), this);
        getServer().getPluginManager().registerEvents(new TeamLifecycleListener(teamDisplay), this);

        teamDisplay.start();
        getServer().getScheduler().runTaskTimer(this, () -> {
            teamManager.cleanupExpired();
            teamManager.save();
        }, 20L * 60L, 20L * 60L * 5L);

        getLogger().info("NethrionTeams enabled — lightweight teams, persistent homes, and optional friendly fire.");
    }

    @Override
    public void onDisable() {
        if (teamDisplay != null) teamDisplay.stop();
        if (teamManager != null) teamManager.save();
    }
}
