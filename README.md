# Spoofax Gradle Plugin

The Spoofax Gradle plugin makes it possible to build Spoofax languages with Gradle.

## Building

To compile and package the project:

```
./gradlew assemble
```

To publish the artifact to Maven local:

```
./gradlew pTML
```

## Usage

Below is an example build script for building a Spoofax language project.

```groovy
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    id 'nl.martijndwars.spoofax' version '1.0-SNAPSHOT'
}

repositories {
    metaborgReleases()
    metaborgSnapshots()
}

spoofax {
  strategoFormat = 'ctree'
  overrides = []
}
```

To build your Spoofax language:

```
gradle archiveLanguage
```

If you get a StackOverflowError or OutOfMemoryError place the following in `gradle.properties`:

```
org.gradle.jvmargs=-Xms1g -Xmx2g -Xss32m
```

## Recipes

### Multi-project build

If you have a multi-project build in which one Spoofax project depends upon another:

```groovy
dependencies {
    compileLanguage project(':projectB')
}
```

### Override versions

To override any of the language dependencies:

```groovy
spoofax {
  overrides = [
    'com.acme:foo.lang:1.2.3'
  ]
}
```

### Publishing (TODO)

If you want to publish the Spoofax language artifact:

```groovy
publishing {
    publications {
        mavenSpoofaxLanguage(MavenPublication) {
            from components.spx
        }
    }
  
    repositories {
        // Your Artifactory.
    }
}
```

(WIP) If you want to force the version of a dependency:

```groovy
configurations.all {
    resolutionStrategy.force 'com.acme:acme-lang:1.2.3'
}
```

## Internals

The plugin modifies the build in several ways.

### Plugins

The plugin applies the [Base plugin](https://docs.gradle.org/current/userguide/base_plugin.html) to the project.

### Tasks

The plugin defines the following tasks:

* `cleanLanguage`: Clean the Spoofax language. This task is made a dependency of `clean`.
* `compileLanguage`: Build the Spoofax language. This task is made a dependency of `assemble`.

### Configurations

The plugin defines two dependency configurations:

* `compileLanguage`
* `sourceLanguage`

The plugin defines one artifact configuration:

* `spoofaxLanguage`

The `spoofaxLanguage` configuration contains the built Spoofax language (.spoofax-language) file. The `assemble` configuration is made to extend the `spoofaxLanguage` configuration.

### Artifacts

The plugin defines an `spx` artifact that contains the result of building the Spoofax language, i.e. the `.spoofax-language` file.

### Software Components

The plugin defines a `components.spx` that contains the `spx` artifact.

