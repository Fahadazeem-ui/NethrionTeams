# Build notes

Requires Java 21.

Linux/macOS:
`./gradlew clean build --no-daemon`

Windows:
`gradlew.bat clean build --no-daemon`

GitHub Actions runs the Linux command after explicitly applying executable permission to `gradlew`.
