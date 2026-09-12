package com.nethrion.teams;
import org.bukkit.command.PluginCommand;import org.bukkit.plugin.java.JavaPlugin;
public final class NethrionTeamsPlugin extends JavaPlugin{
 private TeamManager manager;private TeamDisplay display;private VaultManager vaults;
 public void onEnable(){saveDefaultConfig();manager=new TeamManager(this);vaults=new VaultManager(this,manager);display=new TeamDisplay(this,manager);PluginCommand t=getCommand("team");t.setExecutor(new TeamCommand(manager,display));t.setTabCompleter(new TeamCommand(manager,display));getCommand("war").setExecutor(new WarClaimCommand(manager));StorageCommand storageCommand=new StorageCommand(manager,vaults);getCommand("storage").setExecutor(storageCommand);getServer().getPluginManager().registerEvents(storageCommand,this);getServer().getPluginManager().registerEvents(new VaultListener(this,vaults),this);getServer().getPluginManager().registerEvents(new TeamProtectionListener(manager),this);getServer().getPluginManager().registerEvents(new TeamLifecycleListener(display),this);getServer().getPluginManager().registerEvents(new WarListener(manager),this);display.start();getServer().getScheduler().runTaskTimer(this,()->{manager.cleanupWars();manager.save();},20L,20L);getLogger().info("NethrionTeams enabled.");}
 public void onDisable(){if(display!=null)display.stop();if(manager!=null)manager.save();if(vaults!=null)vaults.save();}
}
