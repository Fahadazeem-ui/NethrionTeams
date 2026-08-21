package com.nethrion.teams;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class TeamLifecycleListener implements Listener {
    private final TeamDisplay display;

    public TeamLifecycleListener(TeamDisplay display) {
        this.display = display;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        display.refresh(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // No volatile state is kept for online players.
    }
}
