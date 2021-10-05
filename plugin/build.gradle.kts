import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("com.vanniktech.maven.publish")
  id("com.github.johnrengelman.shadow").version("7.0.0")
}


dependencies {
  shadow(gradleApi())
  implementation(fileTree("lib") { include("*.jar") })
  implementation(fileTree("${System.getProperty("user.home")}/.konan/kotlin-native-prebuilt-macos-x86_64-1.5.30/konan/lib") { include("*.jar") })
}

val shadowJarTask = tasks.shadowJar

// Add shadow jar to the Gradle module metadata api and runtime configurations
configurations {
  artifacts {
    runtimeElements(shadowJarTask)
    apiElements(shadowJarTask)
  }
}

tasks.whenTaskAdded {
  if (name == "publishPluginJar" || name == "generateMetadataFileForPluginMavenPublication") {
    dependsOn(tasks.named("shadowJar"))
  }
}

// Disabling default jar task as it is overridden by shadowJar
tasks.named("jar").configure {
  enabled = false
}

// Need to move publishing configuration into afterEvaluate {}
// to override changes done by "com.gradle.plugin-publish" plugin in afterEvaluate {} block
// See PublishPlugin class for details
afterEvaluate {
  publishing {
    publications {
      withType<MavenPublication> {
        // Special workaround to publish shadow jar instead of normal one. Name to override peeked here:
        // https://github.com/gradle/gradle/blob/master/subprojects/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L73
        if (name == "pluginMaven") {
          setArtifacts(
            listOf(
              shadowJarTask.get()
            )
          )
        }
      }
    }
  }
}

/*tasks.create("relocateShadowJar", ConfigureShadowRelocation::class.java) {
  target = tasks.shadowJar.get()
  prefix = "cklib" // Default value is "shadow"
}

tasks.shadowJar.dependsOn(tasks.findByName("relocateShadowJar")!!)*/

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