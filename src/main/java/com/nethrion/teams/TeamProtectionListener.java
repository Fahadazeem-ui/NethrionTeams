package com.nethrion.teams;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class TeamProtectionListener implements Listener {
    private final TeamManager manager;

    public TeamProtectionListener(TeamManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        if (!manager.canDamage(attacker.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    private Player resolvePlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }
}
