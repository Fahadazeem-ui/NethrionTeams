package com.nethrion.teams;
import org.bukkit.Location;
import java.util.*;
public final class Team {
 private final UUID id; private String name; private UUID owner; private final LinkedHashSet<UUID> members=new LinkedHashSet<>();
 private boolean pvpEnabled; private Location home,base; private String primary="#00E5FF",secondary="#7C4DFF";
 private long oreMined,playerKills,mobKills,trades;
 public Team(UUID id,String name,UUID owner,boolean pvp){this.id=id;this.name=name;this.owner=owner;this.pvpEnabled=pvp;members.add(owner);}
 public UUID getId(){return id;} public String getName(){return name;} public void setName(String n){name=n;}
 public UUID getOwner(){return owner;} public void setOwner(UUID o){owner=o;members.add(o);}
 public Set<UUID> getMembers(){return Collections.unmodifiableSet(members);} public List<UUID> getMembersSnapshot(){return new ArrayList<>(members);}
 public boolean isPvpEnabled(){return pvpEnabled;} public void setPvpEnabled(boolean v){pvpEnabled=v;}
 public Location getHome(){return home==null?null:home.clone();} public void setHome(Location l){home=l==null?null:l.clone();}
 public Location getBase(){return base==null?null:base.clone();} public void setBase(Location l){base=l==null?null:l.clone();}
 public boolean addMember(UUID u){return members.add(u);} public boolean removeMember(UUID u){return members.remove(u);} public boolean contains(UUID u){return members.contains(u);}
 public String getPrimary(){return primary;} public String getSecondary(){return secondary;} public void setStyle(String p,String s){primary=p;secondary=s;}
 public long getOreMined(){return oreMined;} public long getPlayerKills(){return playerKills;} public long getMobKills(){return mobKills;} public long getTrades(){return trades;}
 public void addOreMined(long n){oreMined=Math.max(0,oreMined+n);} public void addPlayerKills(long n){playerKills=Math.max(0,playerKills+n);} public void addMobKills(long n){mobKills=Math.max(0,mobKills+n);} public void addTrades(long n){trades=Math.max(0,trades+n);}
 public void setStats(long o,long pk,long mk,long tr){oreMined=Math.max(0,o);playerKills=Math.max(0,pk);mobKills=Math.max(0,mk);trades=Math.max(0,tr);}
}
