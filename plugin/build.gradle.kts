plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("com.vanniktech.maven.publish")
}

dependencies {
  implementation(kotlin("gradle-plugin-api"))
  implementation(fileTree("lib") { include("*.jar") })
  implementation(fileTree("/Users/kgalligan/.konan/kotlin-native-prebuilt-macos-x86_64-1.5.30/konan/lib") { include("*.jar") })
}

buildConfig {
  packageName("co.touchlab.cpak.gradle")
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")
}

gradlePlugin {
  plugins {
    create("cpakPlugin") {
      id = rootProject.extra["kotlin_plugin_id"] as String
      displayName = "Cpak Gradle Plugin"
      description = "Cpak Gradle Plugin"
      implementationClass = "co.touchlab.kermit.gradle.KermitGradlePlugin"
    }
  }
}
