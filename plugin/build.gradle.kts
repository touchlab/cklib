import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("com.vanniktech.maven.publish")
}

kotlin {
  jvmToolchain(11)
}

dependencies {
  implementation(gradleApi())
  implementation(libs.kotlin.gradle.plugin)
  implementation(kotlin("stdlib"))
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
repositories {
  mavenCentral()
}
//val compileKotlin: KotlinCompile by tasks
//compileKotlin.kotlinOptions {
//  jvmTarget = "1.8"
//}
//val compileTestKotlin: KotlinCompile by tasks
//compileTestKotlin.kotlinOptions {
//  jvmTarget = "1.8"
//}