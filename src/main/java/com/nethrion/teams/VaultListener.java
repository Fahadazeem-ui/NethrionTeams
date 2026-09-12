package com.nethrion.teams;
import org.bukkit.block.Block;import org.bukkit.event.*;import org.bukkit.event.block.*;import org.bukkit.plugin.java.JavaPlugin;
public final class VaultListener implements Listener{
 private final JavaPlugin plugin;private final VaultManager v;public VaultListener(JavaPlugin p,VaultManager v){plugin=p;this.v=v;}
 @EventHandler(ignoreCancelled=true)public void place(BlockPlaceEvent e){plugin.getServer().getScheduler().runTask(plugin,()->v.tryRegister(e.getPlayer(),e.getBlockPlaced()));}
 @EventHandler(ignoreCancelled=true)public void breakBlock(BlockBreakEvent e){v.invalidateAt(e.getBlock());}
}
