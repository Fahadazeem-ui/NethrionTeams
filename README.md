# NovaEssential 1.0.1

Paper 1.21.11 teleport and moderation plugin.

## Commands
- `/tpa <player>`
- `/tpahere <player>`
- `/tpaaccept [player]` (`/tpaccept`)
- `/tpacancel`
- `/bed`
- `/spawn`
- `/setspawn` (OP)
- `/ban <player> [reason...]`
- `/tempban <player> <duration> <reason...>`
- `/pardon <player>` (`/unban`)

## Defaults
- TPA request expiry: 30 seconds
- TPA warmup: 2 seconds
- Bed warmup: 3 seconds; player damage cancels
- Spawn warmup: 5 seconds; player damage cancels

## Hardening
- TPA state is owned by the server main thread and cleaned centrally.
- Requests are removed on expiry, acceptance, cancellation, death, and quit.
- Warmups are cancelled on death, quit, failed state, or any external teleport.
- Async teleport completion is marshalled back to the main thread before touching Bukkit player state.
- `/bed` resolves the current respawn location at completion and verifies a nearby bed, so a changed/destroyed bed is respected.
- Offline moderation lookup avoids `getOfflinePlayer(String)` network lookups and scans cached/local player data instead.
- Player profile bans use Paper's profile-aware ban API; the plugin does not maintain a second custom ban database.
- Ban UI uses restrained NovaEssential + gray/aqua styling instead of rainbow/gradient formatting.
- No periodic world or entity scans are used.
