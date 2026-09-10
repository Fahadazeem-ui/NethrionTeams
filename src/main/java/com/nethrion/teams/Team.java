package com.nethrion.teams;
import org.bukkit.Location;
import java.util.*;
public final class Team {
 private String name; private UUID owner; private final LinkedHashSet<UUID> members=new LinkedHashSet<>();
 private boolean pvpEnabled; private Location home; private String color="AQUA";
 private long ores,mobs,players,trades;
 public Team(String name, UUID owner, boolean pvp){this.name=name;this.owner=owner;this.pvpEnabled=pvp;members.add(owner);}
 public String getName(){return name;} public void setName(String n){name=n;}
 public UUID getOwner(){return owner;} public void setOwner(UUID o){owner=o;members.add(o);}
 public Set<UUID> getMembers(){return Collections.unmodifiableSet(members);} public List<UUID> getMembersSnapshot(){return new ArrayList<>(members);}
 public boolean isPvpEnabled(){return pvpEnabled;} public void setPvpEnabled(boolean v){pvpEnabled=v;}
 public Location getHome(){return home==null?null:home.clone();} public void setHome(Location h){home=h==null?null:h.clone();}
 public boolean addMember(UUID u){return members.add(u);} public boolean removeMember(UUID u){return members.remove(u);} public boolean contains(UUID u){return members.contains(u);}
 public String getColor(){return color;} public void setColor(String c){color=c;}
 public synchronized void addOre(){ores++;} public synchronized void addMob(){mobs++;} public synchronized void addPlayerKill(){players++;} public synchronized void addTrade(){trades++;}
 public synchronized long score(){return ores*3L+mobs+players*10L+trades*2L;}
 public synchronized long[] stats(){return new long[]{ores,mobs,players,trades};}
 public synchronized void setStats(long a,long b,long c,long d){ores=Math.max(0,a);mobs=Math.max(0,b);players=Math.max(0,c);trades=Math.max(0,d);}
}