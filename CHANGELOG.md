# 1.2.3

* Upgrade: Spoofax to 2.5.11.

# 1.2.2

* Upgrade: Spoofax to 2.5.9.

# 1.2.1

* Upgrade: Spoofax to 2.5.7.

# 1.2.0

* Upgrade: Spoofax to 2.5.4.
* Improvement: Include `*.flo` in input of language compilation task.

# 1.1.0

* Upgrade: Gradle to 5.2.1. Fix deprecation warnings.
* Improvement: Match repositories to artifacts (#5).
* Improvement: Add basic incrementality (no change anywhere -> no rebuild)

# 1.0.2

* Improvement: Apply the Java plugin instead of the Base plugin. Run `compileJava` between `compileLanguage` and `archiveLanguage`.
* Improvement: Configure `compileJava` to use the ECJ compiler (instead of default JDK compiler) to compile the huge mess of Spoofax-generated Java.
* Improvement: Add a `compileOnly` dependency on `org.metaborg.spoofax.core`, since the Spoofax-generated Java needs to be compiled against the Spoofax API.
* Improvement: Modify the generated `editor.esv.af` to use the semantics provider that was configured in the Gradle build (i.e. jar or ctree).
* Improvement: Hijack the functionality in `StrategoRuntimeFacetFromESV` to return the .jar or .ctree file depending on the configured strategoFormat (i.e. jar or ctree).
* Improvement: Add shorthand `spoofaxRepos` to add all repositories that are needed for a Spoofax build (metaborg releases, metaborg snapshots, sugar-lang, pluto-build, usethesource) at once.

# 1.0.1

* Bugfix: Add repositories that are needed to build the plugin (Metaborg, Pluto Build, Sugar Lang, usethesource) and replace mavenCentral by jcenter.
* Improvement: Add a test dependency on JUnit 5 (Jupiter API & engine), update and move the example projects to the test resources directory, and configure Gradle to run the tests.
* Improvement: Have every project use it's own Spoofax instance such that we can safely build multiple languages in parallel.

# 1.0.0

Initial release.
