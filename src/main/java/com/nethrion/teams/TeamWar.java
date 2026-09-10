package com.nethrion.teams;
import java.util.*;
public final class TeamWar {
 public enum Status {PENDING,ACTIVE}
 private final UUID id; private final UUID creator; private final LinkedHashSet<UUID> targets; private final LinkedHashSet<UUID> accepted;
 public TeamWar(UUID id,UUID creator,Collection<UUID> targets,Collection<UUID> accepted){this.id=id;this.creator=creator;this.targets=new LinkedHashSet<>(targets);this.accepted=new LinkedHashSet<>(accepted);}
 public UUID id(){return id;} public UUID creator(){return creator;} public Set<UUID> targets(){return Collections.unmodifiableSet(targets);}
 public boolean accept(UUID team){if(!targets.contains(team))return false;accepted.add(team);return true;}
 public boolean isReady(){return accepted.containsAll(targets);} public Status status(){return isReady()?Status.ACTIVE:Status.PENDING;}
 public Set<UUID> accepted(){return Collections.unmodifiableSet(accepted);}
}