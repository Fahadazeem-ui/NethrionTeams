Build/audit notes for 1.0.1

The source was reviewed for:
- stale teleport/request state
- async Bukkit access
- death/quit cleanup
- bed respawn validity
- offline lookup blocking
- duplicate request state
- moderation persistence/integration
- permission and command argument handling
- message styling

Live Paper dependency resolution is environment-dependent. Use `./gradlew clean build --no-daemon --no-configuration-cache --refresh-dependencies` in CI or a local Paper build environment.
