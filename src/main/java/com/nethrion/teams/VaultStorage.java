package com.nethrion.teams;
import java.io.File;import java.util.*;import org.bukkit.*;import org.bukkit.configuration.ConfigurationSection;import org.bukkit.configuration.file.YamlConfiguration;import org.bukkit.plugin.java.JavaPlugin;
public final class VaultStorage{
 private final JavaPlugin plugin;private final File file;
 public VaultStorage(JavaPlugin p){plugin=p;file=new File(p.getDataFolder(),"vaults.yml");}
 private String loc(Location l){return l.getWorld().getUID()+"|"+l.getBlockX()+"|"+l.getBlockY()+"|"+l.getBlockZ();}
 private Location parse(String s){try{String[] a=s.split("\\|");World w=null;UUID id=UUID.fromString(a[0]);for(World x:Bukkit.getWorlds())if(x.getUID().equals(id)){w=x;break;}return w==null?null:new Location(w,Integer.parseInt(a[1]),Integer.parseInt(a[2]),Integer.parseInt(a[3]));}catch(Exception e){return null;}}
 public Map<UUID,TeamVault> load(){Map<UUID,TeamVault> r=new LinkedHashMap<>();YamlConfiguration y=YamlConfiguration.loadConfiguration(file);ConfigurationSection s=y.getConfigurationSection("vaults");if(s==null)return r;for(String k:s.getKeys(false))try{ConfigurationSection x=s.getConfigurationSection(k);Location a=parse(x.getString("left","")),b=parse(x.getString("right",""));if(a!=null&&b!=null)r.put(UUID.fromString(k),new TeamVault(UUID.fromString(k),UUID.fromString(x.getString("team")),UUID.fromString(x.getString("creator")),a,b,x.getLong("created")));}catch(Exception ignored){}return r;}
 public void save(Collection<TeamVault> vs){YamlConfiguration y=new YamlConfiguration();for(TeamVault v:vs){String b="vaults."+v.id();y.set(b+".team",v.teamId().toString());y.set(b+".creator",v.creatorId().toString());y.set(b+".left",loc(v.left()));y.set(b+".right",loc(v.right()));y.set(b+".created",v.createdAt());}try{y.save(file);}catch(Exception e){plugin.getLogger().warning("Could not save vaults: "+e.getMessage());}}
}
