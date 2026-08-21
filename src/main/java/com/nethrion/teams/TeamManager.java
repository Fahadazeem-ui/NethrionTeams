package com.nethrion.teams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TeamManager {
    private final JavaPlugin plugin;
    private final TeamStorage storage;
    private final Map<String, Team> teams;
    private final Map<String, Map<UUID, Long>> requests;
    private final Map<String, Map<UUID, Long>> invites;

    private final long requestLifetimeMillis;
    private final long inviteLifetimeMillis;
    private final int minNameLength;
    private final int maxNameLength;
    private final boolean defaultPvp;

    public TeamManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storage = new TeamStorage(plugin);
        this.teams = new LinkedHashMap<>(storage.loadTeams());
        this.requests = new LinkedHashMap<>(storage.loadRequestMap("requests"));
        this.invites = new LinkedHashMap<>(storage.loadRequestMap("invites"));

        this.requestLifetimeMillis = plugin.getConfig().getLong("settings.request-expiration-days", 3) * 86_400_000L;
        this.inviteLifetimeMillis = plugin.getConfig().getLong("settings.invitation-expiration-days", 3) * 86_400_000L;
        this.minNameLength = plugin.getConfig().getInt("settings.min-team-name-length", 3);
        this.maxNameLength = plugin.getConfig().getInt("settings.max-team-name-length", 16);
        this.defaultPvp = plugin.getConfig().getBoolean("settings.default-pvp", false);
    }

    public synchronized void save() {
        storage.saveAll(teams, requests, invites);
    }

    public synchronized Team findByName(String name) {
        return teams.get(TeamStorage.normalize(name));
    }

    public synchronized Team findByPlayer(UUID uuid) {
        for (Team team : teams.values()) {
            if (team.contains(uuid)) return team;
        }
        return null;
    }

    public synchronized boolean createTeam(Player owner, String rawName) {
        String name = rawName.trim();
        if (!isValidTeamName(name) || findByName(name) != null || findByPlayer(owner.getUniqueId()) != null) {
            return false;
        }
        teams.put(TeamStorage.normalize(name), new Team(name, owner.getUniqueId(), defaultPvp));
        save();
        return true;
    }

    public boolean isValidTeamName(String name) {
        return name.length() >= minNameLength
                && name.length() <= maxNameLength
                && name.matches("[A-Za-z0-9_-]+");
    }

    public synchronized boolean addInvite(Player target, Team team) {
        cleanupExpired();
        if (findByPlayer(target.getUniqueId()) != null || team.contains(target.getUniqueId())) return false;
        String targetKey = target.getUniqueId().toString();
        UUID marker = markerForTeam(team);
        invites.computeIfAbsent(targetKey, ignored -> new LinkedHashMap<>()).put(marker, System.currentTimeMillis());
        save();
        return true;
    }

    public synchronized boolean hasInvite(Player target, Team team) {
        cleanupExpired();
        Map<UUID, Long> map = invites.get(target.getUniqueId().toString());
        return map != null && map.containsKey(markerForTeam(team));
    }

    public synchronized boolean acceptInvite(Player player, Team team) {
        cleanupExpired();
        if (team == null || findByPlayer(player.getUniqueId()) != null) return false;
        Map<UUID, Long> map = invites.get(player.getUniqueId().toString());
        UUID marker = team == null ? null : markerForTeam(team);
        if (map == null || marker == null || !map.containsKey(marker)) return false;

        team.addMember(player.getUniqueId());
        map.remove(marker);
        if (map.isEmpty()) invites.remove(player.getUniqueId().toString());
        save();
        return true;
    }

    public synchronized boolean requestJoin(Player player, Team team) {
        cleanupExpired();
        if (team == null || findByPlayer(player.getUniqueId()) != null) return false;
        requests.computeIfAbsent(TeamStorage.normalize(team.getName()), ignored -> new LinkedHashMap<>())
                .put(player.getUniqueId(), System.currentTimeMillis());
        save();
        return true;
    }

    public synchronized boolean hasRequest(Team team, UUID requester) {
        cleanupExpired();
        Map<UUID, Long> map = requests.get(TeamStorage.normalize(team.getName()));
        return map != null && map.containsKey(requester);
    }

    public synchronized boolean acceptRequest(Player owner, UUID requester) {
        cleanupExpired();
        Team team = ownerTeam(owner);
        Map<UUID, Long> map = requests.get(TeamStorage.normalize(team.getName()));
        if (map == null || !map.containsKey(requester) || findByPlayer(requester) != null) return false;
        team.addMember(requester);
        map.remove(requester);
        if (map.isEmpty()) requests.remove(TeamStorage.normalize(team.getName()));
        save();
        return true;
    }

    public synchronized boolean denyRequest(Player owner, UUID requester) {
        Team team = ownerTeam(owner);
        Map<UUID, Long> map = requests.get(TeamStorage.normalize(team.getName()));
        if (map == null || !map.containsKey(requester)) return false;
        map.remove(requester);
        if (map.isEmpty()) requests.remove(TeamStorage.normalize(team.getName()));
        save();
        return true;
    }

    public synchronized List<UUID> pendingRequests(Team team) {
        cleanupExpired();
        Map<UUID, Long> map = requests.get(TeamStorage.normalize(team.getName()));
        return map == null ? List.of() : new ArrayList<>(map.keySet());
    }

    public synchronized boolean setHome(Player owner) {
        Team team = ownerTeam(owner);
        team.setHome(owner.getLocation());
        save();
        return true;
    }

    public synchronized boolean deleteHome(Player owner) {
        Team team = ownerTeam(owner);
        team.setHome(null);
        save();
        return true;
    }

    public synchronized boolean togglePvp(Player owner) {
        Team team = ownerTeam(owner);
        team.setPvpEnabled(!team.isPvpEnabled());
        save();
        return team.isPvpEnabled();
    }

    public synchronized boolean canDamage(UUID first, UUID second) {
        Team a = findByPlayer(first);
        Team b = findByPlayer(second);
        return a == null || b == null || a != b || a.isPvpEnabled();
    }

    public synchronized boolean leave(Player player) {
        Team team = findByPlayer(player.getUniqueId());
        if (team == null) return false;
        boolean ownerLeaving = team.getOwner().equals(player.getUniqueId());
        team.removeMember(player.getUniqueId());

        if (team.getMembers().isEmpty()) {
            teams.remove(TeamStorage.normalize(team.getName()));
            requests.remove(TeamStorage.normalize(team.getName()));
            UUID marker = markerForTeam(team);
            invites.values().forEach(map -> map.remove(marker));
            invites.values().removeIf(Map::isEmpty);
        } else if (ownerLeaving) {
            team.setOwner(team.getMembers().iterator().next());
        }

        save();
        return true;
    }

    public synchronized boolean disband(Player owner) {
        Team team = ownerTeam(owner);
        String key = TeamStorage.normalize(team.getName());
        UUID marker = markerForTeam(team);
        teams.remove(key);
        requests.remove(key);
        invites.values().forEach(map -> map.remove(marker));
        invites.values().removeIf(Map::isEmpty);
        save();
        return true;
    }

    public synchronized void cleanupExpired() {
        long now = System.currentTimeMillis();
        requests.values().forEach(map -> map.entrySet().removeIf(entry -> now - entry.getValue() > requestLifetimeMillis));
        requests.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        invites.values().forEach(map -> map.entrySet().removeIf(entry -> now - entry.getValue() > inviteLifetimeMillis));
        invites.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public synchronized Team currentTeam(Player player) {
        return findByPlayer(player.getUniqueId());
    }

    public void sendTeamMessage(Team team, String message) {
        for (UUID uuid : team.getMembers()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.sendMessage(message);
        }
    }

    public synchronized Team ownerTeam(Player player) {
        Team team = findByPlayer(player.getUniqueId());
        if (team == null || !team.getOwner().equals(player.getUniqueId())) throw new IllegalStateException("OWNER_ONLY");
        return team;
    }

    public synchronized String formatHome(Team team) {
        Location home = team.getHome();
        if (home == null || home.getWorld() == null) return ChatColor.GRAY + "No team home is set.";
        return ChatColor.DARK_GRAY + "Home " + ChatColor.GRAY + home.getWorld().getName()
                + ChatColor.DARK_GRAY + " • " + ChatColor.WHITE
                + home.getBlockX() + " " + home.getBlockY() + " " + home.getBlockZ()
                + ChatColor.DARK_GRAY + " • " + ChatColor.GRAY + "Meet at our home.";
    }

    public synchronized List<String> formatMembers(Team team) {
        return team.getMembers().stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .filter(name -> name != null)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public synchronized List<String> teamNames() {
        return teams.values().stream().map(Team::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    public synchronized String teamTag(Player player) {
        Team team = findByPlayer(player.getUniqueId());
        return team == null ? "" : team.getName();
    }

    private UUID markerForTeam(Team team) {
        return UUID.nameUUIDFromBytes(team.getName().toLowerCase(java.util.Locale.ROOT).getBytes(StandardCharsets.UTF_8));
    }
}
