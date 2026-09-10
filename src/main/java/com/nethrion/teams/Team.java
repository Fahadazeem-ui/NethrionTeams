package com.nethrion.teams;
import org.bukkit.Location;
import java.util.*;
public final class Team {
 private final UUID id; private String name; private UUID owner; private final LinkedHashSet<UUID> members=new LinkedHashSet<>();
 private boolean pvpEnabled; private Location home; private String primary="#00E5FF", secondary="#7C4DFF";
 public Team(UUID id,String name,UUID owner,boolean pvp){this.id=id;this.name=name;this.owner=owner;this.pvpEnabled=pvp;members.add(owner);}
 public UUID getId(){return id;} public String getName(){return name;} public void setName(String n){name=n;}
 public UUID getOwner(){return owner;} public void setOwner(UUID o){owner=o;members.add(o);}
 public Set<UUID> getMembers(){return Collections.unmodifiableSet(members);} public List<UUID> getMembersSnapshot(){return new ArrayList<>(members);}
 public boolean isPvpEnabled(){return pvpEnabled;} public void setPvpEnabled(boolean v){pvpEnabled=v;}
 public Location getHome(){return home==null?null:home.clone();} public void setHome(Location h){home=h==null?null:h.clone();}
 public boolean addMember(UUID u){return members.add(u);} public boolean removeMember(UUID u){return members.remove(u);} public boolean contains(UUID u){return members.contains(u);}
 public String getPrimary(){return primary;} public String getSecondary(){return secondary;}
 public void setStyle(String p,String s){primary=p;secondary=s;}
}