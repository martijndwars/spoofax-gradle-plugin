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

## Usage

Below is an example build script for building a Spoofax language project.

```groovy
plugins {
    id 'nl.martijndwars.spoofax' version '1.0.1'
}

repositories {
    metaborgReleases()
    metaborgSnapshots()
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

The plugin applies the [Base plugin](https://docs.gradle.org/current/userguide/base_plugin.html) to the project.

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
