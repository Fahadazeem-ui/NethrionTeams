package com.nethrion.teams;

import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.MerchantInventory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight event-driven team statistics collector.
 * Merchant trades are counted only when the result slot is clicked in a merchant inventory.
 */
public final class TeamStatsListener implements Listener {
    private static final long PLAYER_PAIR_COOLDOWN_MS = 300_000L;
    private static final long TRADE_COOLDOWN_MS = 750L;

    private final TeamManager manager;
    private final Map<UUID, Long> tradeCooldown = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> playerPairCooldown = new HashMap<>();

    public TeamStatsListener(TeamManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        String name = event.getBlock().getType().name();
        if (name.endsWith("_ORE")) {
            manager.addOre(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        if (event.getEntity() instanceof Player victim) {
            if (victim.getUniqueId().equals(killer.getUniqueId())) return;
            long now = System.currentTimeMillis();
            Map<UUID, Long> victims = playerPairCooldown.computeIfAbsent(
                    killer.getUniqueId(), ignored -> new HashMap<>());
            long last = victims.getOrDefault(victim.getUniqueId(), 0L);
            if (now - last < PLAYER_PAIR_COOLDOWN_MS) return;
            victims.put(victim.getUniqueId(), now);
            manager.addPlayerKill(killer.getUniqueId());
        } else if (!(event.getEntity() instanceof ArmorStand)) {
            manager.addMob(killer.getUniqueId());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMerchantClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory() instanceof MerchantInventory)) return;
        if (event.getSlotType() != InventoryType.SlotType.RESULT) return;

        long now = System.currentTimeMillis();
        long last = tradeCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < TRADE_COOLDOWN_MS) return;
        tradeCooldown.put(player.getUniqueId(), now);
        manager.addTrade(player.getUniqueId());
    }
}
