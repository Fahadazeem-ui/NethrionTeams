package com.nethrion.teams;
import org.bukkit.*;import org.bukkit.entity.Player;import org.bukkit.plugin.java.JavaPlugin;import java.util.*;
public final class TeamManager{
 private final JavaPlugin plugin;private final TeamStorage storage;private final Map<String,Team> teams=new LinkedHashMap<>();private final Map<UUID,Team> index=new HashMap<>();
 private final int min,max;private final boolean defaultPvp;
 public TeamManager(JavaPlugin p){plugin=p;storage=new TeamStorage(p);teams.putAll(storage.loadTeams());for(Team t:teams.values())for(UUID u:t.getMembers())index.put(u,t);min=p.getConfig().getInt("settings.min-team-name-length",3);max=p.getConfig().getInt("settings.max-team-name-length",16);defaultPvp=p.getConfig().getBoolean("settings.default-pvp",false);}
 public synchronized void save(){storage.saveAll(teams,Map.of(),Map.of());}
 public synchronized Team findByName(String n){return n==null?null:teams.get(TeamStorage.normalize(n));} public synchronized Team findByPlayer(UUID u){return index.get(u);} public synchronized Team currentTeam(Player p){return index.get(p.getUniqueId());}
 public boolean isValidTeamName(String n){return n!=null&&n.length()>=min&&n.length()<=max&&n.matches("[A-Za-z0-9_-]+");}
 public synchronized boolean createTeam(Player o,String n){if(!isValidTeamName(n)||findByName(n)!=null||index.containsKey(o.getUniqueId()))return false;Team t=new Team(n,o.getUniqueId(),defaultPvp);teams.put(TeamStorage.normalize(n),t);index.put(o.getUniqueId(),t);save();return true;}
 public synchronized boolean rename(Player p,String n){Team t=ownerTeam(p);if(!isValidTeamName(n)||findByName(n)!=null)return false;teams.remove(TeamStorage.normalize(t.getName()));t.setName(n);teams.put(TeamStorage.normalize(n),t);save();return true;}
 public synchronized boolean setColor(Player p,String c){Team t=ownerTeam(p);if(!List.of("AQUA","BLUE","DARK_PURPLE","LIGHT_PURPLE","GOLD","GREEN","RED","YELLOW").contains(c))return false;t.setColor(c);save();return true;}
 public synchronized boolean setHome(Player p){ownerTeam(p).setHome(p.getLocation());save();return true;} public synchronized boolean deleteHome(Player p){ownerTeam(p).setHome(null);save();return true;}
 public synchronized boolean togglePvp(Player p){Team t=ownerTeam(p);t.setPvpEnabled(!t.isPvpEnabled());save();return t.isPvpEnabled();}
 public synchronized boolean canDamage(UUID a,UUID b){Team x=index.get(a),y=index.get(b);return x==null||y==null||x!=y||x.isPvpEnabled();}
 public synchronized boolean leave(Player p){Team t=index.remove(p.getUniqueId());if(t==null)return false;boolean own=t.getOwner().equals(p.getUniqueId());t.removeMember(p.getUniqueId());if(t.getMembers().isEmpty())teams.remove(TeamStorage.normalize(t.getName()));else if(own)t.setOwner(t.getMembers().iterator().next());save();return true;}
 public synchronized boolean disband(Player p){Team t=ownerTeam(p);teams.remove(TeamStorage.normalize(t.getName()));for(UUID u:t.getMembers())index.remove(u);save();return true;}
 public synchronized Team ownerTeam(Player p){Team t=index.get(p.getUniqueId());if(t==null||!t.getOwner().equals(p.getUniqueId()))throw new IllegalStateException("OWNER_ONLY");return t;}
 public synchronized List<String> teamNames(){return teams.values().stream().map(Team::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();}
 public synchronized List<Team> board(){return teams.values().stream().sorted(Comparator.comparingLong(Team::score).reversed().thenComparing(Team::getName,String.CASE_INSENSITIVE_ORDER)).toList();}
 public List<String> formatMembers(Team t){return t.getMembers().stream().map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).filter(Objects::nonNull).sorted(String.CASE_INSENSITIVE_ORDER).toList();}
 public void addOre(UUID u){Team t=index.get(u);if(t!=null)t.addOre();} public void addMob(UUID u){Team t=index.get(u);if(t!=null)t.addMob();} public void addPlayerKill(UUID u){Team t=index.get(u);if(t!=null)t.addPlayerKill();} public void addTrade(UUID u){Team t=index.get(u);if(t!=null)t.addTrade();}
 public String teamTag(Player p){Team t=currentTeam(p);if(t==null)return "";try{return org.bukkit.ChatColor.valueOf(t.getColor())+""+org.bukkit.ChatColor.BOLD+t.getName();}catch(Exception e){return org.bukkit.ChatColor.AQUA+""+org.bukkit.ChatColor.BOLD+t.getName();}} public void cleanupExpired(){} public void sendTeamMessage(Team t,String m){for(UUID u:t.getMembers()){Player p=Bukkit.getPlayer(u);if(p!=null)p.sendMessage(m);}}
}