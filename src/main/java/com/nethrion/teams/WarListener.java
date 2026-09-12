package com.nethrion.teams;
import org.bukkit.*;import org.bukkit.entity.Player;import org.bukkit.event.*;import org.bukkit.event.entity.PlayerDeathEvent;
public final class WarListener implements Listener{
 private final TeamManager m;public WarListener(TeamManager m){this.m=m;}
 @EventHandler public void death(PlayerDeathEvent e){Player v=e.getEntity();Player k=v.getKiller();if(k==null)return;TeamWar w=m.recordWarKill(k,v);if(w!=null&&w.status()==TeamWar.Status.COMPLETED){Team win=m.findById(w.winner());String n=win==null?"Unknown":win.getName();for(Player p:Bukkit.getOnlinePlayers())p.showTitle(net.kyori.adventure.title.Title.title(net.kyori.adventure.text.Component.text("⚔ WAR ENDED ⚔"),net.kyori.adventure.text.Component.text(n+" WON")));}}
}
