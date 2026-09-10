package com.nethrion.teams;
import org.bukkit.command.PluginCommand;import org.bukkit.plugin.java.JavaPlugin;
public final class NethrionTeamsPlugin extends JavaPlugin{
 private TeamManager teamManager;private TeamDisplay teamDisplay;
 public void onEnable(){saveDefaultConfig();teamManager=new TeamManager(this);teamDisplay=new TeamDisplay(this,teamManager);PluginCommand cmd=getCommand("team");if(cmd==null){getServer().getPluginManager().disablePlugin(this);return;}TeamCommand ex=new TeamCommand(teamManager,teamDisplay);cmd.setExecutor(ex);cmd.setTabCompleter(ex);getServer().getPluginManager().registerEvents(new TeamProtectionListener(teamManager),this);getServer().getPluginManager().registerEvents(new TeamLifecycleListener(teamDisplay),this);teamDisplay.start();getServer().getScheduler().runTaskTimer(this,teamManager::save,20L*300,20L*300);getLogger().info("NethrionTeams 2.0 enabled.");}
 public void onDisable(){if(teamDisplay!=null)teamDisplay.stop();if(teamManager!=null)teamManager.save();}
}