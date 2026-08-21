# NethrionTeams

A small, vanilla-friendly team plugin for Paper 1.21.11.

## Philosophy
Teams are social groups, not classes. A team can contain builders, PvPers, redstoners, miners, farmers, or players who do several things. The plugin does not assign professions or restrict member roles.

The visible identity is intentionally minimal: when enabled, a muted team suffix is appended to the existing player-list name instead of replacing another plugin's rank/title name.

## Commands

- `/team create <name>` — create a team.
- `/team invite <username>` — invite a player. Invitations expire after 3 days.
- `/team request <team>` — request to join. Requests expire after 3 days.
- `/team accept <target>` — accept an invite by team name, or accept a join request by username when you are the owner.
- `/team deny <username>` — owner denies a pending join request.
- `/team sethome` — owner stores one team-home location.
- `/team home` — shows the saved coordinates; it never teleports.
- `/team delhome` — owner clears the team-home slot.
- `/team pvp` — owner toggles friendly fire for team members.
- `/team info [team]` — show team information.
- `/team members` — show the current roster.
- `/team leave` — leave the current team. The owner automatically transfers ownership to the oldest remaining member.
- `/team disband` — owner-only disband.
- `/team help` — clean command guide.

## Build

```text
gradle build
```

The jar is written to `build/libs/NethrionTeams-1.0.0.jar`.
