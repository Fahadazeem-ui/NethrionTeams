package com.nethrion.teams;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class TeamStorage {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public TeamStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "teams.yml");
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder");
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public Map<String, Team> loadTeams() {
        Map<String, Team> result = new LinkedHashMap<>();
        ConfigurationSection section = data.getConfigurationSection("teams");
        if (section == null) return result;

        for (String id : section.getKeys(false)) {
            ConfigurationSection teamSection = section.getConfigurationSection(id);
            if (teamSection == null) continue;
            String name = teamSection.getString("name", id);
            String ownerString = teamSection.getString("owner");
            if (ownerString == null) continue;

            UUID owner;
            try {
                owner = UUID.fromString(ownerString);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Skipping team with invalid owner UUID: " + name);
                continue;
            }

            Team team = new Team(name, owner, teamSection.getBoolean("pvp", false));
            team.getMembers().clear();
            for (String memberString : teamSection.getStringList("members")) {
                try {
                    team.addMember(UUID.fromString(memberString));
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Ignoring invalid member UUID in team " + name);
                }
            }
            team.addMember(owner);

            ConfigurationSection homeSection = teamSection.getConfigurationSection("home");
            if (homeSection != null) {
                String worldName = homeSection.getString("world");
                if (worldName != null && plugin.getServer().getWorld(worldName) != null) {
                    Location home = new Location(
                            plugin.getServer().getWorld(worldName),
                            homeSection.getDouble("x"),
                            homeSection.getDouble("y"),
                            homeSection.getDouble("z"),
                            (float) homeSection.getDouble("yaw"),
                            (float) homeSection.getDouble("pitch")
                    );
                    team.setHome(home);
                }
            }

            result.put(normalize(name), team);
        }
        return result;
    }

    public Map<String, Map<UUID, Long>> loadRequestMap(String root) {
        Map<String, Map<UUID, Long>> result = new LinkedHashMap<>();
        ConfigurationSection section = data.getConfigurationSection(root);
        if (section == null) return result;

        for (String key : section.getKeys(false)) {
            ConfigurationSection nested = section.getConfigurationSection(key);
            if (nested == null) continue;
            Map<UUID, Long> values = new LinkedHashMap<>();
            for (String uuidString : nested.getKeys(false)) {
                try {
                    values.put(UUID.fromString(uuidString), nested.getLong(uuidString));
                } catch (IllegalArgumentException ignored) {
                }
            }
            result.put(key, values);
        }
        return result;
    }

    public void saveAll(Map<String, Team> teams,
                        Map<String, Map<UUID, Long>> requests,
                        Map<String, Map<UUID, Long>> invites) {
        data.set("teams", null);
        data.set("requests", null);
        data.set("invites", null);

        for (Team team : teams.values()) {
            String base = "teams." + normalize(team.getName());
            data.set(base + ".name", team.getName());
            data.set(base + ".owner", team.getOwner().toString());
            data.set(base + ".members", team.getMembersSnapshot().stream().map(UUID::toString).toList());
            data.set(base + ".pvp", team.isPvpEnabled());

            Location home = team.getHome();
            if (home != null && home.getWorld() != null) {
                data.set(base + ".home.world", home.getWorld().getName());
                data.set(base + ".home.x", home.getX());
                data.set(base + ".home.y", home.getY());
                data.set(base + ".home.z", home.getZ());
                data.set(base + ".home.yaw", home.getYaw());
                data.set(base + ".home.pitch", home.getPitch());
            }
        }

        saveRequestMap("requests", requests);
        saveRequestMap("invites", invites);

        try {
            data.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save teams.yml", ex);
        }
    }

    private void saveRequestMap(String root, Map<String, Map<UUID, Long>> values) {
        for (Map.Entry<String, Map<UUID, Long>> outer : values.entrySet()) {
            for (Map.Entry<UUID, Long> inner : outer.getValue().entrySet()) {
                data.set(root + "." + outer.getKey() + "." + inner.getKey(), inner.getValue());
            }
        }
    }

    public static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
