**I no longer maintain this repository. This project has been superseded by https://github.com/metaborg/spoofax.gradle/.**

# Spoofax Gradle Plugin

The Spoofax Gradle plugin makes it possible to build [Spoofax](https://www.metaborg.org/) languages with Gradle.

[![Build Status](https://travis-ci.com/MartijnDwars/spoofax-gradle-plugin.svg?token=shoMhBh2Xrkx994EwmBt&branch=master)](https://travis-ci.com/MartijnDwars/spoofax-gradle-plugin)
[![Gradle Plugin Portal](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/nl/martijndwars/spoofax/nl.martijndwars.spoofax.gradle.plugin/maven-metadata.xml.svg?label=gradle)](https://plugins.gradle.org/plugin/nl.martijndwars.spoofax)

## Building

To compile and package the plugin:

```
gradle assemble
```

To publish the plugin to your local Maven repository:

```
gradle pTML
```

On macOS Catalina, run the build/tests in a Docker container:

```
cat << EOF > Dockerfile
FROM container-registry.oracle.com/java/serverjre:8
WORKDIR /root
ENTRYPOINT ./gradlew test
EOF

docker build -t spoofax-gradle-plugin .
docker run -v /path/to/spoofax-gradle-plugin:/root spoofax-gradle-plugin:latest
```

## Usage

Below is an example build script for building a Spoofax language project.

```groovy
plugins {
    id 'nl.martijndwars.spoofax' version '1.2.4'
}

repositories {
    jcenter()
    spoofaxRepos()
}

spoofax {
    strategoFormat = 'ctree'
    languageVersion = '0.1.0-SNAPSHOT'
    overrides = []
}
```

To build your Spoofax language:

```
gradle archiveLanguage
```

If you get a StackOverflowError or OutOfMemoryError create a `gradle.properties` with the following content:

```
org.gradle.jvmargs=-Xms1g -Xmx2g -Xss32m
```

## Versions

| Plugin version | Spoofax version |
| -------------- | --------------- |
| 1.2.5          | 2.5.16          |
| 1.2.4          | 2.5.11          |
| 1.2.3          | 2.5.11          |
| 1.2.2          | 2.5.9           |
| 1.2.1          | 2.5.7           |
| 1.2.0          | 2.5.4           |
| 1.0.0 - 1.1.0  | 2.5.1           |

## Recipes

### Multi-Project Build

If you have a [source dependency](http://www.metaborg.org/en/latest/source/core/manual/concepts.html?highlight=source%20dependency) on another project in a multi-project build:

```groovy
dependencies {
    sourceLanguage project(':projectB')
}
```

### Parallel Builds

If you have a multi-project build, then you might be able to build projects that do not directly depend upon each other in parallel.
To enable parallel task execution, use the `--parallel` flag or add the following to the project's `gradle.properties`:

```
org.gradle.parallel=true
```

### Language Testing

Assume you have a project `foo.lang` that defines the language and contains one or more .spt files.
If you add a compile dependency on the SPT language to `metaborg.yaml`, then the `checkLanguage` task runs the SPT tests.
For example,

```yaml
dependencies:
  compile:
  - org.metaborg:org.metaborg.meta.lang.spt:${metaborgVersion}
```

Assume you have a project `foo.lang` that defines the language and `foo.tests` that contains one or more .spt files.
In `foo.tests/metaborg.yaml` add a compile dependency on SPT and a source dependency on the language under test.
In `foo.tests/build.gradle` add a project-dependency on `:foo.lang` and configure the `checkLanguage` task by specifying the language under test.

```yaml
---
id: org.example:foo.tests:0.1.0-SNAPSHOT
name: foo.tests
dependencies:
  compile:
  - org.metaborg:org.metaborg.meta.lang.spt:${metaborgVersion}
  source:
  - org.example:foo.lang:0.1.0-SNAPSHOT
```

```groovy
dependencies {
    sourceLanguage project(':foo.lang')
}

checkLanguage {
    languageUnderTest = "org.example:foo.lang:$version"
}
```

### Language Publishing

The Spoofax plugin integrates with Gradle's `maven-publish` plugin. Add the following to your buildscript:

```groovy
plugins {
    id 'maven-publish'
}

publishing {
    publications {
        simple(MavenPublication) { publication ->
            project.spoofax.component(publication)
        }
    }

    repositories {
        maven {
            url "https://repository.acme.com"
        }
    }
}
```

This creates a Maven publication based on your Spoofax language specification.
In particular, the publication's `groupId`, `artifactId`, and `version` are based on the language specification, not on the Gradle project.
Run `gradle pTML` or `gradle publish` to publish the Spoofax language as Maven publication to your local Maven repository or remote Maven repository, respectively.

### Override versions

To override the version of a source dependency:

```groovy
spoofax {
    overrides = [
        'com.acme:foo.lang:1.2.3'
    ]
}
```

## Internals

The plugin modifies the build in several ways.

### Plugins

The plugin applies the [Java plugin](https://docs.gradle.org/current/userguide/java_plugin.html) to your project, which in turn applies the [Base plugin](https://docs.gradle.org/current/userguide/base_plugin.html) to your project.

### Tasks

The plugin defines the following tasks:

* `cleanLanguage`: Clean the Spoofax language. This task is a dependency of `clean`.
* `compileLanguage`: Compile the Spoofax language. This task is a dependency of `assemble`.
* `archiveLanguage`: Archive the Spoofax language. This task depends on `compileLanguage` and is a dependency of `assemble`.
* `checkLanguage`: Check the Spoofax language. This task depends on `archiveLanguage` and is a dependency of `check`. The property `languageUnderTest` defines the language to be tested. If this property is not specified, the current language is tested.

If `:projectA` has a [project lib dependency](https://docs.gradle.org/current/userguide/multi_project_builds.html#sec:project_jar_dependencies) on `:projectB` in the `sourceLanguage` configuration, then `:projectA:compileLanguage` depends on `:projectB:archiveLanguage`.
This ensures that all project dependencies are built before the depending project is built.

### Configurations

The plugin defines three dependency configurations:

* `compileLanguage`: a configuration holding all the compile dependencies.
* `sourceLanguage`: a configuration holding all the source dependencies.
* `language`: a convenience configuration that extends `compileLanguage` and `sourceLanguage`.

The plugin defines one artifact configuration:

* `spoofaxLanguage`

The `spoofaxLanguage` configuration contains the built Spoofax language (.spoofax-language) artifact.
The `assemble` configuration is made to extend the `spoofaxLanguage` configuration.

## Java Compilation

The `compileLanguage` task generates Java sources and the `archiveLanguage` task expects the generated Java sources to be compiled.
Hence, the plugin configures `compileJava` to depend on `compileLanguage` and to be a dependency of `archiveLanguage`.

A moderately large Spoofax project generates _many_ Java files.
In fact, Spoofax generates so many Java files that the default JDK compiler runs out of memory (even with `-Xmx4g`).
For this reason, the plugin configures `compileJava` to use the the [Eclipse Java Compiler (ECJ)](https://www.eclipse.org/jdt/core/).

The generated Java sources need to be compiled against the Spoofax API.
For this reason, the plugin adds a `compileOnly` dependency on `org.metaborg.spoofax.core`.
A consequence is that a plugin user needs to add repositories in which all transitive dependencies can be resolved.

## Repositories

The plugin adds a `spoofaxRepos()` function. This function adds the following repositories:

* [Metaborg Releases](https://artifacts.metaborg.org/content/repositories/releases/)
* [Metaborg Snapshots](https://artifacts.metaborg.org/content/repositories/snapshots/)
* [Pluto Build](https://pluto-build.github.io/mvnrepository/)
* [Sugar Lang](https://sugar-lang.github.io/mvnrepository/)
* [UseTheSource](http://nexus.usethesource.io/content/repositories/public/)

You will need to add a repository where third-party dependencies can be resolved, e.g. `jcenter()` or `mavenCentral()`.
It is recommended to add the `spoofaxRepos()` _before_ any other repository, because:

- The repositories that are added by `spoofaxRepos()` are specific for the artifacts they contain. For example, the Metaborg repositories will only be contacted for artifacts with group `org.metaborg`. That is, there is no penalty for adding `spoofaxRepos()` first.
- Third-party repositories do not by default exclude artifacts that are only available in the Spoofax repositories. If a third-party repository is added first, then Gradle will first check if a Spoofax artifact exists in this repository. That is, there is a penalty for adding third-party repositories first.

