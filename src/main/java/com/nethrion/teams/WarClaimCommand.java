package com.nethrion.teams;
import org.bukkit.command.*;import org.bukkit.entity.Player;
public final class WarClaimCommand implements CommandExecutor{private final TeamManager m;public WarClaimCommand(TeamManager m){this.m=m;}public boolean onCommand(CommandSender s,Command c,String l,String[] a){if(s instanceof Player p)p.sendMessage(m.claim(p));return true;}}
