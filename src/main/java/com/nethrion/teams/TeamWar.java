package com.nethrion.teams;
import java.util.*;
public final class TeamWar {
 public enum Status {PENDING,ACTIVE,COMPLETED,EXPIRED,CANCELLED}
 private final UUID id,creator; private final LinkedHashSet<UUID> teams=new LinkedHashSet<>(),accepted=new LinkedHashSet<>();
 private final long createdAt; private long inviteExpiresAt,startedAt,expiresAt,endedAt;
 private Status status; private UUID winner,loser; private final Map<UUID,LinkedHashSet<UUID>> kills=new HashMap<>(); private final Set<UUID> claimed=new HashSet<>();
 public TeamWar(UUID id,UUID creator,Collection<UUID> teams,Collection<UUID> accepted,long created,long inviteExpiry,long started,long expiry,long ended,Status status,UUID winner,UUID loser,Map<UUID,? extends Collection<UUID>> killMap,Collection<UUID> claimed){
  this.id=id;this.creator=creator;this.teams.addAll(teams);this.accepted.addAll(accepted);this.createdAt=created;this.inviteExpiresAt=inviteExpiry;this.startedAt=started;this.expiresAt=expiry;this.endedAt=ended;this.status=status;this.winner=winner;this.loser=loser;
  if(killMap!=null)killMap.forEach((k,v)->this.kills.put(k,new LinkedHashSet<>(v)));if(claimed!=null)this.claimed.addAll(claimed);
 }
 public UUID id(){return id;} public UUID creator(){return creator;} public Set<UUID> teams(){return Collections.unmodifiableSet(teams);} public Set<UUID> targets(){return teams();} public Set<UUID> accepted(){return Collections.unmodifiableSet(accepted);}
 public long createdAt(){return createdAt;} public long inviteExpiresAt(){return inviteExpiresAt;} public long startedAt(){return startedAt;} public long expiresAt(){return expiresAt;} public long endedAt(){return endedAt;} public Status status(){return status;} public UUID winner(){return winner;} public UUID loser(){return loser;}
 public boolean accept(UUID team,long now,long duration){if(status!=Status.PENDING||now>=inviteExpiresAt||!teams.contains(team))return false;accepted.add(team);if(accepted.containsAll(teams)){status=Status.ACTIVE;startedAt=now;expiresAt=now+duration;}return true;}
 public void expireInvite(long now){if(status==Status.PENDING&&now>=inviteExpiresAt){status=Status.CANCELLED;endedAt=now;}}
 public void expireWar(long now){if(status==Status.ACTIVE&&now>=expiresAt){status=Status.EXPIRED;endedAt=now;}}
 public boolean recordKill(UUID killerTeam,UUID victim,Set<UUID> opponentMembers,long now){if(status!=Status.ACTIVE||killerTeam==null||opponentMembers==null||opponentMembers.isEmpty())return false;kills.computeIfAbsent(killerTeam,k->new LinkedHashSet<>()).add(victim);if(kills.get(killerTeam).containsAll(opponentMembers)){winner=killerTeam;status=Status.COMPLETED;endedAt=now;return true;}return false;}
 public Set<UUID> killsBy(UUID team){return Collections.unmodifiableSet(kills.getOrDefault(team,new LinkedHashSet<>()));}
 public Map<UUID,Set<UUID>> allKills(){Map<UUID,Set<UUID>> r=new LinkedHashMap<>();kills.forEach((k,v)->r.put(k,Set.copyOf(v)));return r;}
 public boolean canClaim(UUID player){return status==Status.COMPLETED&&winner!=null&&!claimed.contains(player);} public boolean markClaimed(UUID player){return claimed.add(player);} public Set<UUID> claimed(){return Collections.unmodifiableSet(claimed);}
 public static TeamWar pending(UUID creator,Collection<UUID> teams,long now,long inviteMs){return new TeamWar(UUID.randomUUID(),creator,teams,List.of(creator),now,now+inviteMs,0,0,0,Status.PENDING,null,null,null,null);}
}
