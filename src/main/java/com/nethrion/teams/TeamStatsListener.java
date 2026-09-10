package com.nethrion.teams;
import org.bukkit.entity.*;import org.bukkit.event.*;import org.bukkit.event.block.BlockBreakEvent;import org.bukkit.event.entity.EntityDeathEvent;import org.bukkit.event.player.PlayerTradeEvent;import java.util.*;
public final class TeamStatsListener implements Listener{
 private final TeamManager m;private final Map<UUID,Long> playerKillCooldown=new HashMap<>();private final Map<UUID,Long> tradeCooldown=new HashMap<>();
 public TeamStatsListener(TeamManager m){this.m=m;}
 @EventHandler(ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(e.getBlock().getType().name().endsWith("_ORE"))m.addOre(e.getPlayer().getUniqueId());}
 @EventHandler(ignoreCancelled=true) public void death(EntityDeathEvent e){Player k=e.getEntity().getKiller();if(k==null)return;if(e.getEntity() instanceof Player victim){if(victim.equals(k))return;long now=System.currentTimeMillis(),last=playerKillCooldown.getOrDefault(victim.getUniqueId(),0L);if(now-last<300000L)return;playerKillCooldown.put(victim.getUniqueId(),now);m.addPlayerKill(k.getUniqueId());}else if(!(e.getEntity() instanceof ArmorStand))m.addMob(k.getUniqueId());}
 @EventHandler(ignoreCancelled=true) public void trade(PlayerTradeEvent e){long now=System.currentTimeMillis(),last=tradeCooldown.getOrDefault(e.getPlayer().getUniqueId(),0L);if(now-last<1000L)return;tradeCooldown.put(e.getPlayer().getUniqueId(),now);m.addTrade(e.getPlayer().getUniqueId());}
}