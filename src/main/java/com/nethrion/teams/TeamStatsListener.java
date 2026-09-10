package com.nethrion.teams;
import org.bukkit.Material;import org.bukkit.entity.*;import org.bukkit.event.*;import org.bukkit.event.block.BlockBreakEvent;import org.bukkit.event.entity.*;import org.bukkit.event.inventory.*;import org.bukkit.event.player.PlayerTradeEvent;
public final class TeamStatsListener implements Listener{
 private final TeamManager m;public TeamStatsListener(TeamManager m){this.m=m;}
 @EventHandler(ignoreCancelled=true) public void breakBlock(BlockBreakEvent e){if(e.getBlock().getType().name().endsWith("_ORE"))m.addOre(e.getPlayer().getUniqueId());}
 @EventHandler(ignoreCancelled=true) public void death(EntityDeathEvent e){Player p=e.getEntity().getKiller();if(p==null)return;if(e.getEntity() instanceof Player)m.addPlayerKill(p.getUniqueId());else m.addMob(p.getUniqueId());}
 @EventHandler(ignoreCancelled=true) public void trade(PlayerTradeEvent e){m.addTrade(e.getPlayer().getUniqueId());}
}