# 1.0.1-SNAPSHOT

* Bugfix: Add repositories that are needed to build the plugin (Metaborg, Pluto Build, Sugar Lang, usethesource) and replace mavenCentral by jcenter.
* Improvement: Add a test dependency on JUnit 5 (Jupiter API & engine), update and move the example projects to the test resources directory, and configure Gradle to run the tests.
* Improvement: Have every project use it's own Spoofax instance such that we can safely build multiple languages in parallel.

# 1.0.0

Initial release.
