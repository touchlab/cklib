## The Problem

When you want to access C-etc code from Kotlin/Native, you point the cinterop process at it. That will create Kotlin code to enable
calling the native code. That's only one part of the process, though. You *also* need to build and link binaries to implement
the native calls. Configuring native builds can be complex because of the number of options that need to be correctly configured,
as well as the need to package and link the complied binaries.

## The Solution

This problem is already kind of solved by Kotlin/Native itself. The platform is based largely on C and C++. There's a [Gradle
plugin](https://github.com/JetBrains/kotlin/blob/7b73917217de6dc66330593887c44e67a4efb7d3/kotlin-native/build-tools/src/main/kotlin/org/jetbrains/kotlin/bitcode/CompileToBitcodePlugin.kt)
and set of tasks embedded within Kotlin/Native and the broader Kotlin platform that configures and builds native C-etc code.
That plugin is not published in an accessible way, unfortunately. CKlib extracts that Clang Gradle plugin to be used externally.

For packaging, the problem is already solved there as well. You can just insert the compiled binary into your klib. CKlib
is configured to do that for you.

## Status

This plugin hasn't been designed for all use cases. We needed to compile and embed C code with no real external dependencies.
You may want to build and embed C-etc code for other use cases we haven't considered, and this plugin will almost certainly
need to be modified for your particular situation. Please start conversations and/or submit PRs if you add anything significant.

## Usage

Add the plugin to your buildscript path:

```kotlin
buildscript {
  repositories {
    mavenCentral() // <- need this
    google()
    gradlePluginPortal()
  }
  dependencies {
    classpath("co.touchlab:cklib-gradle-plugin:1.5.31.3") // <- Replace with current version
  }
}
```

Apply the plugin. You will also need to have the Kotlin Multiplatform plugin applied. CKlib depends on it.

```kotlin
plugins {
  kotlin("multiplatform")
  id("co.touchlab.cklib")
}
```

To create compilations, add the `cklib` block, set the Kotlin version, and then point at C-etc source.

```kotlin
cklib {
  config.kotlinVersion = "1.6.0"  
  create("somecode") {
    language = C
    compilerArgs.addAll(
      listOf(
        "-Wno-unused-function"
      )
    )
  }
}
```

By default, the C-etc code is built and packaged in compatible Kotlin/Native klibs. You can specify source folders and
you will likely need to add some compiler args. Anything more custom will probably require tweaking the plugin itself, as
it was really designed for a very particular use case.

## We're Hiring!

Touchlab is looking for a Mobile Developer, with Android/Kotlin experience, who is eager to dive into Kotlin Multiplatform Mobile (KMM) development. Come join the remote-first team putting KMM in production. [More info here](https://go.touchlab.co/careers-gh).

## Primary Maintainer

[Kevin Galligan](https://github.com/kpgalligan/)

![Image of Kevin](https://avatars.githubusercontent.com/u/68384?s=140&v=4)

*Ping me on twitter [@kpgalligan](https://twitter.com/kpgalligan/) if you don't get a timely reply!* -Kevin