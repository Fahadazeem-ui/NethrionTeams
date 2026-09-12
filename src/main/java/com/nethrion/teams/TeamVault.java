package com.nethrion.teams;
import java.util.UUID;
import org.bukkit.Location;
public record TeamVault(UUID id, UUID teamId, UUID creatorId, Location left, Location right, long createdAt) {
 public boolean matches(Location l){return same(left,l)||same(right,l);}
 private boolean same(Location a,Location b){return a!=null&&b!=null&&a.getWorld()!=null&&a.getWorld().equals(b.getWorld())&&a.getBlockX()==b.getBlockX()&&a.getBlockY()==b.getBlockY()&&a.getBlockZ()==b.getBlockZ();}
}
