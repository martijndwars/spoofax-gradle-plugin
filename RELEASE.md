# Release

1. Update the version string in `build.gradle` from -SNAPSHOT to a final version. For example, `version '1.0.0-SNAPSHOT'` becomes `version '1.0.0'`.
2. Commit the changes with message "Release 1.0.0".
3. Tag this commit with the new version "1.0.0".
4. Publish the plugin to Gradle's plugin portal with `gradle publishPlugins`.
5. Bump to the next -SNAPSHOT version. For example, `version '1.0.0'` becomes `version '2.0.0-SNAPSHOT'`.
6. Commit the changes with message "Set version to 2.0.0-SNAPSHOT".