import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation
import org.jetbrains.kotlin.utils.addToStdlib.cast

plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("com.vanniktech.maven.publish")
//  id("maven-publish")
  id("com.github.johnrengelman.shadow").version("7.0.0")
}



//val bigJar by configurations.creating {
////  extendsFrom(configurations.implementation.get())
//}

//dependencies {
//  testImplementation("junit:junit:4.13")
//  smokeTest("org.apache.httpcomponents:httpclient:4.5.5")
//}

dependencies {
  shadow(kotlin("gradle-plugin-api"))
  implementation(fileTree("lib") { include("*.jar") })
//  bigJar(fileTree("lib") { include("*.jar") })
  implementation(fileTree("${System.getProperty("user.home")}/.konan/kotlin-native-prebuilt-macos-x86_64-1.5.30/konan/lib") { include("*.jar") })
//  bigJar(fileTree("${System.getProperty("user.home")}/.konan/kotlin-native-prebuilt-macos-x86_64-1.5.30/konan/lib") { include("*.jar") })
}

//tasks.shadowJar {
//  configurations = listOf(bigJar)
//}

/*tasks.create("relocateShadowJar", ConfigureShadowRelocation::class.java) {
  target = tasks.shadowJar.get()
  prefix = "cpak" // Default value is "shadow"
}

tasks.shadowJar.dependsOn(tasks.findByName("relocateShadowJar")!!)*/

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
      implementationClass = "co.touchlab.cpak.gradle.CompileToBitcodePlugin"
    }
  }
}

/*publishing {
  publications {
    register("mavenJava", MavenPublication::class) {
      project.shadow.component(this)

    }
    repositories {
      maven {
        // change to point to your repo, e.g. http://my.org/repo
        url = uri("$buildDir/repo")
      }
    }
//    register("mavenPublish").cast<MavenPublication>().apply {
//      project.shadow.component(this)
//    }
  }
}*/

publishing {
  publications {
    /*create<MavenPublication>("maven") {
//            groupId = group
//            artifactId = "library"
//            version = version

      components.forEach { println("Components ${it.name}") }
//      from(components["shadow"])
//      artifact(tasks["shadowJar"])
//      artifact(shadowJar)
//      from(components["shadowJar"])
    }*/
  }
}

afterEvaluate {
  publishing.publications.forEach { pub ->
    if(pub is MavenPublication) {
      println("publications ${pub.name}, artifact count ${pub.artifacts.size}")
      pub.artifacts.forEach { println("\t...${it.extension}/${it.classifier}...${it.file}") }
    }
  }
}

//apply(from = "../gradle/shadow-publish.gradle")


//    create<MavenPublication>("shadow") {
//      from(components["java"])
//      artifact(tasks["shadowJar"])
//      project.shadow.component(this)
//    }
//    shadow(MavenPublication) { publication ->
//      project.shadow.component(publication)
//    }

/*
publishing {
  publications {
    // this tells maven-publish to publish our shadow jar
    mavenPublish(MavenPublication) { publication ->
      project.shadow.component(publication)
    }
  }
  // set up local repo for publishing just so we can verify and debug - you normally don't need this
  repositories {
    maven {
      url = "file://${buildDir}/repo/maven"
    }
  }
}
 */

/*sourceSets {
  named("main") {
    withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
      // Gradle Kotlin for JVM plugin configures "src/main/kotlin" on its own
      kotlin.srcDirs("src/main/kotlin", "/Users/kgalligan/temp3/kotlin/kotlin-native/build-tools/src/main/kotlin")
    }
  }
}*/
