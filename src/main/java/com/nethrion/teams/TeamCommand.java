package com.nethrion.teams;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TeamCommand implements CommandExecutor, TabCompleter {
    private static final String PRIMARY = ChatColor.WHITE + "NethrionTeams " + ChatColor.DARK_GRAY + "• ";
    private static final String MUTED = ChatColor.GRAY.toString();

    private final TeamManager manager;
    private final TeamDisplay display;

    public TeamCommand(TeamManager manager, TeamDisplay display) {
        this.manager = manager;
        this.display = display;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /team.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            return switch (sub) {
                case "create" -> create(player, args);
                case "invite" -> invite(player, args);
                case "request" -> request(player, args);
                case "accept" -> accept(player, args);
                case "deny" -> deny(player, args);
                case "sethome" -> setHome(player);
                case "home" -> showHome(player);
                case "delhome" -> deleteHome(player);
                case "pvp" -> pvp(player);
                case "info" -> info(player, args);
                case "members" -> members(player, args);
                case "leave" -> leave(player);
                case "disband" -> disband(player);
                default -> {
                    sendHelp(player);
                    yield true;
                }
            };
        } catch (IllegalStateException ex) {
            if ("OWNER_ONLY".equals(ex.getMessage())) {
                player.sendMessage(PRIMARY + ChatColor.RED + "Owner only.");
            } else {
                player.sendMessage(PRIMARY + ChatColor.RED + "That action is not available.");
            }
            return true;
        }
    }

    private boolean create(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(PRIMARY + MUTED + "/team create <name>");
            return true;
        }
        if (!manager.isValidTeamName(args[1])) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Name must be 3–16 letters, numbers, _ or -.");
            return true;
        }
        if (!manager.createTeam(player, args[1])) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Team name is taken, or you're already in a team.");
            return true;
        }
        display.refresh(player);
        player.sendMessage(PRIMARY + ChatColor.GREEN + "Created " + ChatColor.WHITE + args[1] + ChatColor.GREEN + ".");
        return true;
    }

    private boolean invite(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(PRIMARY + MUTED + "/team invite <username>");
            return true;
        }
        Team team = manager.currentTeam(player);
        if (team == null) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Create or join a team first.");
            return true;
        }
        if (!team.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Only the owner can invite.");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(PRIMARY + ChatColor.RED + "That player is not online.");
            return true;
        }
        if (!manager.addInvite(target, team)) {
            player.sendMessage(PRIMARY + ChatColor.RED + "They already have a team or invite.");
            return true;
        }
        player.sendMessage(PRIMARY + ChatColor.GREEN + "Invite sent to " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + ".");
        target.sendMessage(PRIMARY + ChatColor.WHITE + team.getName() + ChatColor.GRAY + " invited you. " + ChatColor.WHITE + "/team accept " + team.getName());
        target.sendMessage(ChatColor.DARK_GRAY + "   Quiet roster, real teammates.");
        return true;
    }

    private boolean request(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(PRIMARY + MUTED + "/team request <team>");
            return true;
        }
        Team team = manager.findByName(args[1]);
        if (team == null) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Team not found.");
            return true;
        }
        if (!manager.requestJoin(player, team)) {
            player.sendMessage(PRIMARY + ChatColor.RED + "You're already in a team.");
            return true;
        }
        Player owner = Bukkit.getPlayer(team.getOwner());
        if (owner != null) {
            owner.sendMessage(PRIMARY + ChatColor.WHITE + player.getName() + ChatColor.GRAY + " requested to join. " + ChatColor.WHITE + "/team accept " + player.getName());
            owner.sendMessage(ChatColor.DARK_GRAY + "   Request expires in 3 days.");
        }
        player.sendMessage(PRIMARY + ChatColor.GREEN + "Request sent to " + ChatColor.WHITE + team.getName() + ChatColor.GREEN + ".");
        return true;
    }

    private boolean accept(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(PRIMARY + MUTED + "/team accept <team|username>");
            return true;
        }

        Team namedTeam = manager.findByName(args[1]);
        if (namedTeam != null && manager.acceptInvite(player, namedTeam)) {
            display.refresh(player);
            player.sendMessage(PRIMARY + ChatColor.GREEN + "Joined " + ChatColor.WHITE + namedTeam.getName() + ChatColor.GREEN + ".");
            manager.sendTeamMessage(namedTeam, ChatColor.DARK_GRAY + "• " + ChatColor.GRAY + player.getName() + " joined the team.");
            return true;
        }

        Team current = manager.currentTeam(player);
        if (current != null && current.getOwner().equals(player.getUniqueId())) {
            OfflinePlayer requester = Bukkit.getOfflinePlayer(args[1]);
            if (manager.acceptRequest(player, requester.getUniqueId())) {
                display.refreshIfOnline(requester.getUniqueId());
                player.sendMessage(PRIMARY + ChatColor.GREEN + "Accepted " + ChatColor.WHITE + (requester.getName() == null ? args[1] : requester.getName()) + ChatColor.GREEN + ".");
                Player online = Bukkit.getPlayer(requester.getUniqueId());
                if (online != null) {
                    online.sendMessage(PRIMARY + ChatColor.GREEN + "You're in " + ChatColor.WHITE + current.getName() + ChatColor.GREEN + ".");
                    display.refresh(online);
                }
                return true;
            }
        }

        player.sendMessage(PRIMARY + ChatColor.RED + "No matching invite or request found.");
        return true;
    }

    private boolean deny(Player player, String[] args) {
        if (args.length != 2) {
            player.sendMessage(PRIMARY + MUTED + "/team deny <username>");
            return true;
        }
        Team current = manager.currentTeam(player);
        if (current == null || !current.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Owner only.");
            return true;
        }
        OfflinePlayer requester = Bukkit.getOfflinePlayer(args[1]);
        if (!manager.denyRequest(player, requester.getUniqueId())) {
            player.sendMessage(PRIMARY + ChatColor.RED + "No pending request from that player.");
            return true;
        }
        player.sendMessage(PRIMARY + ChatColor.GRAY + "Request denied.");
        if (requester.isOnline()) {
            Player online = requester.getPlayer();
            if (online != null) online.sendMessage(PRIMARY + ChatColor.GRAY + "Your request was declined.");
        }
        return true;
    }

    private boolean setHome(Player player) {
        manager.setHome(player);
        player.sendMessage(PRIMARY + ChatColor.GREEN + "Team home saved here.");
        return true;
    }

    private boolean showHome(Player player) {
        Team team = manager.currentTeam(player);
        if (team == null) {
            player.sendMessage(PRIMARY + ChatColor.RED + "You're not in a team.");
            return true;
        }
        player.sendMessage(manager.formatHome(team));
        return true;
    }

    private boolean deleteHome(Player player) {
        manager.deleteHome(player);
        player.sendMessage(PRIMARY + ChatColor.GREEN + "Team home slot cleared.");
        return true;
    }

    private boolean pvp(Player player) {
        boolean enabled = manager.togglePvp(player);
        Team team = manager.currentTeam(player);
        manager.sendTeamMessage(team, PRIMARY + ChatColor.WHITE + "Team PvP " + (enabled ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        return true;
    }

    private boolean info(Player player, String[] args) {
        Team team;
        if (args.length >= 2) {
            team = manager.findByName(args[1]);
        } else {
            team = manager.currentTeam(player);
        }
        if (team == null) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Team not found.");
            return true;
        }
        player.sendMessage(ChatColor.DARK_GRAY + "──── " + ChatColor.WHITE + team.getName() + ChatColor.DARK_GRAY + " ────");
        player.sendMessage(ChatColor.GRAY + "Owner " + ChatColor.WHITE + Bukkit.getOfflinePlayer(team.getOwner()).getName());
        player.sendMessage(ChatColor.GRAY + "Members " + ChatColor.WHITE + team.getMembers().size());
        player.sendMessage(ChatColor.GRAY + "PvP " + (team.isPvpEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
        player.sendMessage(ChatColor.GRAY + "Home " + (team.getHome() == null ? ChatColor.DARK_GRAY + "unset" : ChatColor.GREEN + "saved"));
        if (team.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.GRAY + "Requests " + ChatColor.WHITE + manager.pendingRequests(team).size());
        }
        return true;
    }

    private boolean members(Player player, String[] args) {
        Team team = args.length >= 2 ? manager.findByName(args[1]) : manager.currentTeam(player);
        if (team == null) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Team not found.");
            return true;
        }
        player.sendMessage(ChatColor.DARK_GRAY + "Members " + ChatColor.WHITE + team.getName());
        for (String name : manager.formatMembers(team)) {
            player.sendMessage(ChatColor.GRAY + "• " + ChatColor.WHITE + name);
        }
        return true;
    }

    private boolean leave(Player player) {
        Team team = manager.currentTeam(player);
        if (team == null) {
            player.sendMessage(PRIMARY + ChatColor.RED + "You're not in a team.");
            return true;
        }
        String oldName = team.getName();
        boolean wasOwner = team.getOwner().equals(player.getUniqueId());
        manager.leave(player);
        display.refresh(player);
        player.sendMessage(PRIMARY + ChatColor.GRAY + "You left " + ChatColor.WHITE + oldName + ChatColor.GRAY + ".");
        if (wasOwner) player.sendMessage(ChatColor.DARK_GRAY + "   Ownership moves to the oldest member.");
        return true;
    }

    private boolean disband(Player player) {
        Team team = manager.currentTeam(player);
        if (team == null || !team.getOwner().equals(player.getUniqueId())) {
            player.sendMessage(PRIMARY + ChatColor.RED + "Owner only.");
            return true;
        }
        String name = team.getName();
        List<UUID> members = team.getMembersSnapshot();
        manager.disband(player);
        for (UUID uuid : members) display.refreshIfOnline(uuid);
        player.sendMessage(PRIMARY + ChatColor.GRAY + "Disbanded " + ChatColor.WHITE + name + ChatColor.GRAY + ".");
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.DARK_GRAY + "──── " + ChatColor.WHITE + "Nethrion Teams" + ChatColor.DARK_GRAY + " ────");
        player.sendMessage(ChatColor.GRAY + "/team create <name>  " + ChatColor.DARK_GRAY + "Create");
        player.sendMessage(ChatColor.GRAY + "/team invite <user>  " + ChatColor.DARK_GRAY + "Invite");
        player.sendMessage(ChatColor.GRAY + "/team request <team> " + ChatColor.DARK_GRAY + "Request");
        player.sendMessage(ChatColor.GRAY + "/team accept <x>     " + ChatColor.DARK_GRAY + "Accept");
        player.sendMessage(ChatColor.GRAY + "/team deny <user>     " + ChatColor.DARK_GRAY + "Deny request");
        player.sendMessage(ChatColor.GRAY + "/team sethome        " + ChatColor.DARK_GRAY + "Set home");
        player.sendMessage(ChatColor.GRAY + "/team home           " + ChatColor.DARK_GRAY + "Show coords");
        player.sendMessage(ChatColor.GRAY + "/team delhome        " + ChatColor.DARK_GRAY + "Clear home");
        player.sendMessage(ChatColor.GRAY + "/team pvp            " + ChatColor.DARK_GRAY + "Toggle friendly fire");
        player.sendMessage(ChatColor.GRAY + "/team info [team]    " + ChatColor.DARK_GRAY + "Team details");
        player.sendMessage(ChatColor.GRAY + "/team members        " + ChatColor.DARK_GRAY + "Roster");
        player.sendMessage(ChatColor.GRAY + "/team leave          " + ChatColor.DARK_GRAY + "Leave");
        player.sendMessage(ChatColor.GRAY + "/team disband        " + ChatColor.DARK_GRAY + "Owner only");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) return List.of();
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("create", "invite", "request", "accept", "deny", "sethome", "home", "delhome", "pvp", "info", "members", "leave", "disband", "help"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("request") || sub.equals("info") || sub.equals("accept")) {
                completions.addAll(manager.teamNames());
                if (sub.equals("accept")) {
                    Team current = manager.currentTeam(player);
                    if (current != null && current.getOwner().equals(player.getUniqueId())) {
                        for (UUID uuid : manager.pendingRequests(current)) {
                            OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
                            if (offline.getName() != null) completions.add(offline.getName());
                        }
                    }
                }
            } else if (sub.equals("invite") || sub.equals("deny")) {
                for (Player online : Bukkit.getOnlinePlayers()) completions.add(online.getName());
            }
        }

        String token = args[args.length - 1].toLowerCase(Locale.ROOT);
        return completions.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(token)).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
