package com.nethrion.teams;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public final class Team {
    private final String name;
    private UUID owner;
    private final LinkedHashSet<UUID> members;
    private boolean pvpEnabled;
    private Location home;

    public Team(String name, UUID owner, boolean pvpEnabled) {
        this.name = name;
        this.owner = owner;
        this.pvpEnabled = pvpEnabled;
        this.members = new LinkedHashSet<>();
        this.members.add(owner);
    }

    public String getName() {
        return name;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        this.members.add(owner);
    }

    public LinkedHashSet<UUID> getMembers() {
        return members;
    }

    public List<UUID> getMembersSnapshot() {
        return new ArrayList<>(members);
    }

    public boolean isPvpEnabled() {
        return pvpEnabled;
    }

    public void setPvpEnabled(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public Location getHome() {
        return home == null ? null : home.clone();
    }

    public void setHome(Location home) {
        this.home = home == null ? null : home.clone();
    }

    public boolean addMember(UUID uuid) {
        return members.add(uuid);
    }

    public boolean removeMember(UUID uuid) {
        return members.remove(uuid);
    }

    public boolean contains(UUID uuid) {
        return members.contains(uuid);
    }
}
