package com.novaessential.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class NovaEssentialPlugin extends JavaPlugin implements Listener {
    private static final long TPA_EXPIRY_SECONDS = 30L;
    private static final long TPA_DELAY_SECONDS = 2L;
    private static final long BED_DELAY_SECONDS = 3L;
    private static final long SPAWN_DELAY_SECONDS = 5L;
    private static final int MAX_PENDING = 5;
    private static final long REQUEST_CLEANUP_TICKS = 20L;
    private static final long RTP_COOLDOWN_MILLIS = 60L * 60L * 1000L;
    private static final int RTP_RADIUS = 50_000;
    private final Map<UUID, Long> rtpCooldowns = new ConcurrentHashMap<>();

    private final Map<UUID, Map<UUID, TpaRequest>> incoming = new HashMap<>();
    private final Map<UUID, Map<UUID, TpaRequest>> outgoing = new HashMap<>();
    private final Map<UUID, Warmup> warmups = new HashMap<>();
    private final Map<UUID, PlayerRecord> knownPlayers = new ConcurrentHashMap<>();
    private BukkitTask requestCleanupTask;

    private String prefix;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadState();
        getServer().getPluginManager().registerEvents(this, this);

        register("tpa", new TpaCommand(false));
        register("tpahere", new TpaCommand(true));
        register("tpaaccept", new TpaAcceptCommand());
        register("tpacancel", new TpaCancelCommand());
        register("bed", new BedCommand());
        register("rtp", new RtpCommand());
        register("spawn", new SpawnCommand());
        register("setspawn", new SetSpawnCommand());
        register("ban", new BanCommand(false));
        register("tempban", new BanCommand(true));
        register("pardon", new PardonCommand());

        requestCleanupTask = Bukkit.getScheduler().runTaskTimer(this, this::cleanupRequests, REQUEST_CLEANUP_TICKS, REQUEST_CLEANUP_TICKS);
        getLogger().info("NovaEssential enabled.");
    }

    @Override
    public void onDisable() {
        if (requestCleanupTask != null) requestCleanupTask.cancel();
        for (Warmup warmup : new ArrayList<>(warmups.values())) cancelWarmup(warmup.playerId, false);
        incoming.clear();
        outgoing.clear();
        warmups.clear();
    }

    private void loadState() {
        prefix = "<gray>[</gray><bold><aqua>NovaEssential</aqua></bold><gray>] </gray>";

        var section = getConfig().getConfigurationSection("known-players");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String name = section.getString(key + ".name");
                    if (name != null && !name.isBlank()) knownPlayers.put(uuid, new PlayerRecord(uuid, name));
                } catch (IllegalArgumentException ignored) {
                    getLogger().warning("Ignoring invalid known-player UUID: " + key);
                }
            }
        }
    }

    private void saveKnownPlayer(Player player) {
        knownPlayers.put(player.getUniqueId(), new PlayerRecord(player.getUniqueId(), player.getName()));
        String path = "known-players." + player.getUniqueId();
        getConfig().set(path + ".name", player.getName());
        saveConfig();
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand command = Objects.requireNonNull(getCommand(name), "Missing command in plugin.yml: " + name);
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) command.setTabCompleter(completer);
    }

    private void cleanupRequests() {
        long now = System.currentTimeMillis();
        for (Map<UUID, TpaRequest> requests : new ArrayList<>(incoming.values())) {
            for (TpaRequest request : new ArrayList<>(requests.values())) {
                if (request.expiresAtMillis <= now) removeRequest(request);
            }
        }
    }

    private boolean queueRequest(Player sender, Player target, boolean tpahere) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(text("You cannot teleport to yourself.", NamedTextColor.RED));
            return false;
        }
        if (target.isDead()) {
            sender.sendMessage(text("That player is unavailable.", NamedTextColor.RED));
            return false;
        }

        removeExistingRequest(sender.getUniqueId(), target.getUniqueId());
        Map<UUID, TpaRequest> targetRequests = incoming.computeIfAbsent(target.getUniqueId(), ignored -> new HashMap<>());
        if (targetRequests.size() >= MAX_PENDING) {
            sender.sendMessage(text("That player has too many pending requests.", NamedTextColor.RED));
            return false;
        }

        long expires = System.currentTimeMillis() + TPA_EXPIRY_SECONDS * 1000L;
        TpaRequest request = new TpaRequest(sender.getUniqueId(), target.getUniqueId(), tpahere, expires);
        targetRequests.put(sender.getUniqueId(), request);
        outgoing.computeIfAbsent(sender.getUniqueId(), ignored -> new HashMap<>()).put(target.getUniqueId(), request);

        sender.sendMessage(line("Request sent to ").append(Component.text(target.getName(), NamedTextColor.WHITE)));
        target.sendMessage(line(sender.getName() + (tpahere ? " wants you to teleport to them." : " wants to teleport to you.")));
        target.sendMessage(line("Use /tpaaccept to accept. Use /tpacancel to cancel."));
        return true;
    }

    private TpaRequest findRequest(Player target, String requesterName) {
        Map<UUID, TpaRequest> requests = incoming.get(target.getUniqueId());
        if (requests == null || requests.isEmpty()) return null;
        long now = System.currentTimeMillis();
        TpaRequest result = null;
        for (TpaRequest request : new ArrayList<>(requests.values())) {
            if (request.expiresAtMillis <= now) {
                removeRequest(request);
                continue;
            }
            if (requesterName == null) {
                if (result == null || request.expiresAtMillis > result.expiresAtMillis) result = request;
            } else {
                Player requester = Bukkit.getPlayer(request.senderId);
                if (requester != null && requester.getName().equalsIgnoreCase(requesterName)) return request;
                PlayerRecord record = knownPlayers.get(request.senderId);
                if (record != null && record.name.equalsIgnoreCase(requesterName)) return request;
            }
        }
        return result;
    }

    private void removeExistingRequest(UUID sender, UUID target) {
        Map<UUID, TpaRequest> a = incoming.get(target);
        if (a != null) {
            TpaRequest old = a.remove(sender);
            if (a.isEmpty()) incoming.remove(target);
            if (old != null) {
                Map<UUID, TpaRequest> out = outgoing.get(sender);
                if (out != null) {
                    out.remove(target);
                    if (out.isEmpty()) outgoing.remove(sender);
                }
            }
        }
    }

    private void removeRequest(TpaRequest request) {
        Map<UUID, TpaRequest> targetRequests = incoming.get(request.targetId);
        if (targetRequests != null) {
            targetRequests.remove(request.senderId, request);
            if (targetRequests.isEmpty()) incoming.remove(request.targetId);
        }
        Map<UUID, TpaRequest> senderRequests = outgoing.get(request.senderId);
        if (senderRequests != null) {
            senderRequests.remove(request.targetId, request);
            if (senderRequests.isEmpty()) outgoing.remove(request.senderId);
        }
    }

    private void cancelAllFrom(UUID senderId) {
        Map<UUID, TpaRequest> requests = outgoing.remove(senderId);
        if (requests == null) return;
        for (TpaRequest request : requests.values()) {
            Map<UUID, TpaRequest> targetRequests = incoming.get(request.targetId);
            if (targetRequests != null) {
                targetRequests.remove(senderId, request);
                if (targetRequests.isEmpty()) incoming.remove(request.targetId);
            }
        }
    }

    private void cancelAllTo(UUID targetId) {
        Map<UUID, TpaRequest> requests = incoming.remove(targetId);
        if (requests == null) return;
        for (TpaRequest request : requests.values()) {
            Map<UUID, TpaRequest> senderRequests = outgoing.get(request.senderId);
            if (senderRequests != null) {
                senderRequests.remove(targetId, request);
                if (senderRequests.isEmpty()) outgoing.remove(request.senderId);
            }
        }
    }

    private void startWarmup(Player player, WarmupType type, long seconds, Location initialTarget) {
        cancelWarmup(player.getUniqueId(), false);
        if (seconds <= 0) {
            executeTeleport(player, type, initialTarget);
            return;
        }

        UUID id = player.getUniqueId();
        long endTick = Bukkit.getCurrentTick() + seconds * 20L;
        Warmup warmup = new Warmup(id, type, initialTarget.clone(), endTick);
        warmups.put(id, warmup);
        warmup.task = Bukkit.getScheduler().runTaskTimer(this, () -> tickWarmup(warmup), 0L, 1L);
    }

    private void tickWarmup(Warmup warmup) {
        if (warmups.get(warmup.playerId) != warmup) return;
        Player player = Bukkit.getPlayer(warmup.playerId);
        if (player == null || !player.isOnline() || player.isDead()) {
            cancelWarmup(warmup.playerId, false);
            return;
        }

        long remainingTicks = warmup.endTick - Bukkit.getCurrentTick();
        if (remainingTicks <= 0) {
            warmups.remove(warmup.playerId, warmup);
            if (warmup.task != null) warmup.task.cancel();

            Location target = warmup.target;
            if (warmup.type == WarmupType.BED) {
                target = resolveBedSpawn(player);
                if (target == null) {
                    player.sendMessage(error("Your bed spawn is not set."));
                    return;
                }
            } else if (warmup.type == WarmupType.SPAWN) {
                target = readSpawn();
                if (target == null) {
                    player.sendMessage(error("Server spawn is not set."));
                    return;
                }
            }
            executeTeleport(player, warmup.type, target);
            return;
        }

        long seconds = (remainingTicks + 19L) / 20L;
        String action = warmup.type == WarmupType.BED ? "Bed" : "Spawn";
        player.sendActionBar(Component.text(action + " in " + seconds + "s", NamedTextColor.GRAY));
    }

    private void executeTeleport(Player player, WarmupType type, Location target) {
        if (!isSafeTeleportTarget(target)) {
            player.sendMessage(error("Teleport location is unavailable."));
            return;
        }
        Location destination = target.clone();
        Bukkit.getScheduler().runTask(this, () -> {
            Player current = Bukkit.getPlayer(player.getUniqueId());
            if (current == null || !current.isOnline() || current.isDead()) return;
            current.teleportAsync(destination).whenComplete((success, error) -> Bukkit.getScheduler().runTask(this, () -> {
                if (current.isOnline() && (Boolean.FALSE.equals(success) || error != null)) {
                    current.sendMessage(error("Teleport failed."));
                }
            }));
        });
    }

    private boolean isSafeTeleportTarget(Location location) {
        return location != null && location.getWorld() != null && Double.isFinite(location.getX()) && Double.isFinite(location.getY()) && Double.isFinite(location.getZ());
    }

    private void cancelWarmup(UUID playerId, boolean notify) {
        Warmup warmup = warmups.remove(playerId);
        if (warmup != null && warmup.task != null) warmup.task.cancel();
        if (notify) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) player.sendActionBar(Component.text("Teleport cancelled", NamedTextColor.RED));
        }
    }

    private Location resolveBedSpawn(Player player) {
        // Paper only exposes a respawn location when the player's spawn point is currently usable.
        // Do not reject it based on an arbitrary nearby-bed scan: the safe spawn can be offset
        // from the physical bed and that caused false "not set" messages.
        Location respawn = player.getRespawnLocation();
        if (respawn == null || respawn.getWorld() == null) return null;
        return respawn.clone();
    }

    private boolean hasBedNearby(Location location) {
        if (location.getWorld() == null) return false;
        int bx = location.getBlockX(), by = location.getBlockY(), bz = location.getBlockZ();
        for (int x = bx - 1; x <= bx + 1; x++) {
            for (int y = by - 1; y <= by + 1; y++) {
                for (int z = bz - 1; z <= bz + 1; z++) {
                    if (org.bukkit.Tag.BEDS.isTagged(location.getWorld().getBlockAt(x, y, z).getType())) return true;
                }
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Warmup warmup = warmups.get(player.getUniqueId());
        if (warmup != null && (warmup.type == WarmupType.BED || warmup.type == WarmupType.SPAWN) && !event.isCancelled()) {
            cancelWarmup(player.getUniqueId(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        cancelWarmup(event.getEntity().getUniqueId(), false);
        cancelAllFrom(event.getEntity().getUniqueId());
        cancelAllTo(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(PlayerTeleportEvent event) {
        Warmup warmup = warmups.get(event.getPlayer().getUniqueId());
        if (warmup == null || event.isCancelled()) return;
        cancelWarmup(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        saveKnownPlayer(event.getPlayer());
        cancelWarmup(id, false);
        cancelAllFrom(id);
        cancelAllTo(id);
    }

    private Location readSpawn() {
        String worldName = getConfig().getString("spawn.world");
        if (worldName == null || worldName.isBlank()) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(world,
                getConfig().getDouble("spawn.x"),
                getConfig().getDouble("spawn.y"),
                getConfig().getDouble("spawn.z"),
                (float) getConfig().getDouble("spawn.yaw"),
                (float) getConfig().getDouble("spawn.pitch"));
    }

    private void saveSpawn(Location location) {
        getConfig().set("spawn.world", location.getWorld().getName());
        getConfig().set("spawn.x", location.getX());
        getConfig().set("spawn.y", location.getY());
        getConfig().set("spawn.z", location.getZ());
        getConfig().set("spawn.yaw", location.getYaw());
        getConfig().set("spawn.pitch", location.getPitch());
        saveConfig();
    }

    private Component line(String text) {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("NovaEssential", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY))
                .append(Component.text(text, NamedTextColor.GRAY));
    }

    private Component text(String value, NamedTextColor color) {
        return Component.text(value, color);
    }

    private Component error(String value) {
        return Component.text(value, NamedTextColor.RED);
    }

    private Component banScreen(String playerName, String reason, Instant expires) {
        Component header = Component.text("NovaEssential", NamedTextColor.AQUA, TextDecoration.BOLD)
                .append(Component.text("  ·  Access denied", NamedTextColor.DARK_GRAY));
        Component duration = expires == null
                ? Component.text("Permanent", NamedTextColor.WHITE)
                : Component.text("Until " + formatTime(expires), NamedTextColor.WHITE);
        return header
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("You are banned from this server.", NamedTextColor.WHITE, TextDecoration.BOLD))
                .append(Component.newline())
                .append(Component.text("Player  ", NamedTextColor.DARK_GRAY)).append(Component.text(playerName, NamedTextColor.GRAY))
                .append(Component.newline())
                .append(Component.text("Duration  ", NamedTextColor.DARK_GRAY)).append(duration)
                .append(Component.newline())
                .append(Component.text("Reason  ", NamedTextColor.DARK_GRAY)).append(Component.text(reason, NamedTextColor.GRAY))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("Contact the server staff if this was issued in error.", NamedTextColor.DARK_GRAY));
    }

    private String formatTime(Instant instant) {
        return DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm z", Locale.ENGLISH).withZone(ZoneId.systemDefault()).format(instant);
    }

    private OfflineTarget findTarget(String name) {
        if (name == null || name.isBlank()) return null;

        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return new OfflineTarget(online.getUniqueId(), online.getName());

        for (PlayerRecord record : knownPlayers.values()) {
            if (record.name.equalsIgnoreCase(name)) return new OfflineTarget(record.uuid, record.name);
        }

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(name);
        if (cached != null && cached.hasPlayedBefore()) {
            return new OfflineTarget(cached.getUniqueId(), Objects.requireNonNullElse(cached.getName(), name));
        }

        // One local lookup across server player data; unlike getOfflinePlayer(String), this never calls the Mojang API.
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            String knownName = offline.getName();
            if (knownName != null && knownName.equalsIgnoreCase(name)) {
                return new OfflineTarget(offline.getUniqueId(), knownName);
            }
        }
        return null;
    }

    private Long parseDuration(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().toLowerCase(Locale.ROOT);
        int split = 0;
        while (split < value.length() && Character.isDigit(value.charAt(split))) split++;
        if (split == 0 || split == value.length()) return null;
        long amount;
        try { amount = Long.parseLong(value.substring(0, split)); }
        catch (NumberFormatException ignored) { return null; }
        if (amount <= 0) return null;
        long multiplier = switch (value.substring(split)) {
            case "s" -> 1_000L;
            case "m" -> 60_000L;
            case "h" -> 3_600_000L;
            case "d" -> 86_400_000L;
            case "w" -> 604_800_000L;
            default -> -1L;
        };
        if (multiplier < 0 || amount > Long.MAX_VALUE / multiplier) return null;
        return amount * multiplier;
    }

    private List<String> playerNames(String prefix) {
        String typed = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(typed))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private abstract class PlayerTabCommand implements CommandExecutor, TabCompleter {
        @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            return args.length == 1 ? playerNames(args[0]) : List.of();
        }
    }

    private final class TpaCommand extends PlayerTabCommand {
        private final boolean tpahere;
        private TpaCommand(boolean tpahere) { this.tpahere = tpahere; }
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            Player player = requirePlayer(sender);
            if (player == null) return true;
            if (args.length != 1) { sender.sendMessage(line("Use /" + label + " <player>.")); return true; }
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) { sender.sendMessage(error("Player not found.")); return true; }
            queueRequest(player, target, tpahere);
            return true;
        }
    }

    private final class TpaAcceptCommand extends PlayerTabCommand {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            Player target = requirePlayer(sender);
            if (target == null) return true;
            if (args.length > 1) { sender.sendMessage(line("Use /tpaaccept [player].")); return true; }
            TpaRequest request = findRequest(target, args.length == 1 ? args[0] : null);
            if (request == null) { sender.sendMessage(line("No pending request.")); return true; }
            removeRequest(request);
            Player requester = Bukkit.getPlayer(request.senderId);
            if (requester == null || !requester.isOnline() || requester.isDead()) {
                sender.sendMessage(error("That player is unavailable."));
                return true;
            }
            Player teleporting = request.tpahere ? target : requester;
            Location destination = request.tpahere ? requester.getLocation().clone() : target.getLocation().clone();
            sender.sendMessage(line("Request accepted."));
            requester.sendMessage(line("Request accepted."));
            startWarmup(teleporting, WarmupType.TPA, TPA_DELAY_SECONDS, destination);
            return true;
        }
    }

    private final class TpaCancelCommand implements CommandExecutor {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            Player player = requirePlayer(sender);
            if (player == null) return true;
            if (args.length != 0) { sender.sendMessage(line("Use /tpacancel.")); return true; }
            Map<UUID, TpaRequest> removed = outgoing.remove(player.getUniqueId());
            if (removed == null || removed.isEmpty()) {
                sender.sendMessage(line("No outgoing requests."));
                return true;
            }
            for (TpaRequest request : removed.values()) {
                Map<UUID, TpaRequest> targetRequests = incoming.get(request.targetId);
                if (targetRequests != null) {
                    targetRequests.remove(request.senderId, request);
                    if (targetRequests.isEmpty()) incoming.remove(request.targetId);
                }
            }
            sender.sendMessage(line("Outgoing requests cancelled."));
            return true;
        }
    }

    private Location findSafeRtpLocation(World world) {
        if (world == null) return null;
        int attempts = 80;
        for (int i = 0; i < attempts; i++) {
            int x = java.util.concurrent.ThreadLocalRandom.current().nextInt(-RTP_RADIUS, RTP_RADIUS + 1);
            int z = java.util.concurrent.ThreadLocalRandom.current().nextInt(-RTP_RADIUS, RTP_RADIUS + 1);
            org.bukkit.WorldBorder border = world.getWorldBorder();
            Location probe = new Location(world, x + 0.5, world.getMinHeight(), z + 0.5);
            if (!border.isInside(probe)) continue;

            int y = world.getHighestBlockYAt(x, z);
            if (y <= world.getMinHeight() || y >= world.getMaxHeight() - 2) continue;

            org.bukkit.block.Block ground = world.getBlockAt(x, y, z);
            org.bukkit.Material type = ground.getType();
            if (!type.isSolid() || ground.isLiquid()) continue;

            org.bukkit.block.Block feet = world.getBlockAt(x, y + 1, z);
            org.bukkit.block.Block head = world.getBlockAt(x, y + 2, z);
            if (!feet.isPassable() || !head.isPassable()) continue;

            Location safe = new Location(world, x + 0.5, y + 1.0, z + 0.5);
            // Highest terrain plus two clear blocks guarantees an open surface location, not a cave.
            return safe;
        }
        return null;
    }

    private final class RtpCommand implements CommandExecutor {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            Player player = requirePlayer(sender);
            if (player == null) return true;
            if (args.length != 0) {
                player.sendMessage(line("Use /rtp."));
                return true;
            }

            long now = System.currentTimeMillis();
            long last = rtpCooldowns.getOrDefault(player.getUniqueId(), 0L);
            long remaining = RTP_COOLDOWN_MILLIS - (now - last);
            if (remaining > 0L) {
                long minutes = (remaining + 59_999L) / 60_000L;
                player.sendMessage(error("You can use /rtp again in " + minutes + " minute(s)."));
                return true;
            }

            Location destination = findSafeRtpLocation(player.getWorld());
            if (destination == null) {
                player.sendMessage(error("No safe surface location was found. Try again."));
                return true;
            }

            player.sendActionBar(Component.text("Finding a safe location...", NamedTextColor.GRAY));
            player.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.COMMAND).whenComplete((success, throwable) ->
                    Bukkit.getScheduler().runTask(NovaEssentialPlugin.this, () -> {
                        if (!player.isOnline()) return;
                        if (throwable != null || !Boolean.TRUE.equals(success)) {
                            player.sendMessage(error("Random teleport failed."));
                            return;
                        }
                        rtpCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                        player.sendMessage(line("Teleported to a safe random location."));
                    })
            );
            return true;
        }
    }

    private final class BedCommand implements CommandExecutor {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            Player player = requirePlayer(sender);
            if (player == null) return true;
            if (args.length != 0) { sender.sendMessage(line("Use /bed.")); return true; }
            Location bed = resolveBedSpawn(player);
            if (bed == null) {
                sender.sendMessage(error("Your bed spawn is not set."));
                return true;
            }
            startWarmup(player, WarmupType.BED, BED_DELAY_SECONDS, bed);
            return true;
        }
    }

    private final class SpawnCommand implements CommandExecutor {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            Player player = requirePlayer(sender);
            if (player == null) return true;
            if (args.length != 0) { sender.sendMessage(line("Use /spawn.")); return true; }
            Location spawn = readSpawn();
            if (spawn == null) {
                sender.sendMessage(error("Server spawn is not set."));
                return true;
            }
            startWarmup(player, WarmupType.SPAWN, SPAWN_DELAY_SECONDS, spawn);
            return true;
        }
    }

    private final class SetSpawnCommand implements CommandExecutor {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player player)) { sender.sendMessage(Component.text("This command is player-only.", NamedTextColor.RED)); return true; }
            if (!sender.isOp()) { sender.sendMessage(error("You do not have permission.")); return true; }
            if (args.length != 0) { sender.sendMessage(line("Use /setspawn.")); return true; }
            saveSpawn(player.getLocation());
            sender.sendMessage(line("Spawn updated."));
            return true;
        }
    }

    private final class BanCommand extends PlayerTabCommand {
        private final boolean temporary;
        private BanCommand(boolean temporary) { this.temporary = temporary; }
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission(temporary ? "novaessential.tempban" : "novaessential.ban")) {
                sender.sendMessage(error("You do not have permission."));
                return true;
            }
            if ((temporary && args.length < 3) || (!temporary && args.length < 1)) {
                sender.sendMessage(line(temporary ? "Use /tempban <player> <duration> <reason>." : "Use /ban <player> [reason]."));
                return true;
            }
            OfflineTarget target = findTarget(args[0]);
            if (target == null) { sender.sendMessage(error("Player not found.")); return true; }
            Player targetPlayer = Bukkit.getPlayer(target.uuid);
            if (targetPlayer != null && sender instanceof Player actor && actor.getUniqueId().equals(target.uuid)) {
                sender.sendMessage(error("You cannot ban yourself."));
                return true;
            }

            Instant expires = null;
            int reasonStart = 1;
            if (temporary) {
                Long millis = parseDuration(args[1]);
                if (millis == null) { sender.sendMessage(error("Invalid duration. Use 30s, 10m, 2h, 7d or 2w.")); return true; }
                expires = Instant.now().plusMillis(millis);
                reasonStart = 2;
            }
            String reason = join(args, reasonStart);
            if (reason.isBlank()) reason = "No reason provided.";

            var offline = Bukkit.getOfflinePlayer(target.uuid);
            offline.ban(reason, expires == null ? null : Date.from(expires), sender.getName());
            if (targetPlayer != null && targetPlayer.isOnline()) targetPlayer.kick(banScreen(targetPlayer.getName(), reason, expires));
            sender.sendMessage(line(target.name + (temporary ? " was temp-banned." : " was banned.")));
            return true;
        }
    }

    private final class PardonCommand extends PlayerTabCommand {
        @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!sender.hasPermission("novaessential.pardon")) { sender.sendMessage(error("You do not have permission.")); return true; }
            if (args.length != 1) { sender.sendMessage(line("Use /pardon <player>.")); return true; }
            OfflineTarget target = findTarget(args[0]);
            if (target == null) { sender.sendMessage(error("Player not found.")); return true; }
            var offline = Bukkit.getOfflinePlayer(target.uuid);
            if (!offline.isBanned()) { sender.sendMessage(line("That player is not banned.")); return true; }
            // Paper's profile-ban API is exposed through OfflinePlayer's profile-aware ban list.
            ((org.bukkit.BanList<org.bukkit.profile.PlayerProfile>) Bukkit.getBanList(org.bukkit.BanList.Type.PROFILE)).pardon(offline.getPlayerProfile());
            sender.sendMessage(line("Ban removed."));
            return true;
        }
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(Component.text("This command is player-only.", NamedTextColor.RED));
        return null;
    }

    private String join(String[] args, int start) {
        StringBuilder out = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) out.append(' ');
            out.append(args[i]);
        }
        return out.toString().trim();
    }

    private record TpaRequest(UUID senderId, UUID targetId, boolean tpahere, long expiresAtMillis) {}
    private record PlayerRecord(UUID uuid, String name) {}
    private record OfflineTarget(UUID uuid, String name) {}
    private enum WarmupType { TPA, BED, SPAWN }
    private static final class Warmup {
        private final UUID playerId;
        private final WarmupType type;
        private final Location target;
        private final long endTick;
        private BukkitTask task;
        private Warmup(UUID playerId, WarmupType type, Location target, long endTick) {
            this.playerId = playerId;
            this.type = type;
            this.target = target;
            this.endTick = endTick;
        }
    }
}
