/*
 * Copyright (c) 2021 Touchlab
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package co.touchlab.cpak.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.konan.target.supportedSanitizers
import javax.inject.Inject

open class CompileToBitcodeExtension @Inject constructor(val project: Project) {

    private val targetList = listOf("linux_x64", "macos_x64", "ios_arm64", "ios_arm32", "ios_simulator_arm64", "ios_x64", "watchos_arm32", "watchos_arm64", "watchos_x86", "watchos_x64",
        "watchos_simulator_arm64", "tvos_arm64", "tvos_x64", "tvos_simulator_arm64", "macos_arm64")
//    with(project) {
//        provider { (rootProject.project(":kotlin-native").property("targetList") as? List<*>)?.filterIsInstance<String>() ?: emptyList() } // TODO: Can we make it better?
//    }

    fun create(
        name: String,
        srcDir: java.io.File = project.file("src/$name"),
        outputGroup: String = "main",
        configurationBlock: CompileToBitcode.() -> Unit = {}
    ) {

//        project.logger.warn("create called ${name}, src dir ${srcDir.absolutePath}")

        val platformManager = project.platformManager

        targetList.forEach { targetName ->

            val target = platformManager.targetByName(targetName)
            val sanitizers: List<org.jetbrains.kotlin.konan.target.SanitizerKind?> =
                target.supportedSanitizers() + listOf(null)
            sanitizers.forEach { sanitizer ->
                val taskName = "${targetName}${name.snakeCaseToCamelCase().capitalize()}${suffixForSanitizer(sanitizer)}"
                project.logger.warn("taskName $taskName")
                project.tasks.register(
                    taskName,
                    CompileToBitcode::class.java,
                    srcDir, name, targetName, outputGroup
                ).configure {
                    it.sanitizer = sanitizer
                    it.group = BasePlugin.BUILD_GROUP
                    val sanitizerDescription = when (sanitizer) {
                        null -> ""
                        org.jetbrains.kotlin.konan.target.SanitizerKind.ADDRESS -> " with ASAN"
                        org.jetbrains.kotlin.konan.target.SanitizerKind.THREAD -> " with TSAN"
                    }
                    it.description = "Compiles '$name' to bitcode for $targetName$sanitizerDescription"
//                    dependsOn(":kotlin-native:dependencies:update")
                    it.configurationBlock()
                }
            }
        }
    }

    companion object {

        private fun String.snakeCaseToCamelCase() =
            split('_').joinToString(separator = "") { it.capitalize() }

        fun suffixForSanitizer(sanitizer: org.jetbrains.kotlin.konan.target.SanitizerKind?) =
            when (sanitizer) {
                null -> ""
                org.jetbrains.kotlin.konan.target.SanitizerKind.ADDRESS -> "_ASAN"
                org.jetbrains.kotlin.konan.target.SanitizerKind.THREAD -> "_TSAN"
            }

    }
}