plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("com.vanniktech.maven.publish")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

val KOTLIN_VERSION: String by project

dependencies {
  implementation(gradleApi())
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION")
}

buildConfig {
  packageName("co.touchlab.cklib.gradle")
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")
}

gradlePlugin {
  plugins {
    create("cklibPlugin") {
      id = rootProject.extra["kotlin_plugin_id"] as String
      displayName = "CKlib Gradle Plugin"
      description = "CKlib Gradle Plugin"
      implementationClass = "co.touchlab.cklib.gradle.CompileToBitcodePlugin"
    }
  }
}