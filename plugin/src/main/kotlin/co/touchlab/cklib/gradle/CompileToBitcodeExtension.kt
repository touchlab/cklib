/*
 * Copyright (c) 2021 Touchlab
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package co.touchlab.cklib.gradle

import org.gradle.api.Project
import javax.inject.Inject

open class CompileToBitcodeExtension @Inject constructor(val project: Project) {
/*
    private val targetList = listOf(
        "linux_x64",
        "macos_x64",
        "ios_arm64",
        "ios_arm32",
        "ios_simulator_arm64",
        "ios_x64",
        "watchos_arm32",
        "watchos_arm64",
        "watchos_x86",
        "watchos_x64",
        "watchos_simulator_arm64",
        "tvos_arm64",
        "tvos_x64",
        "tvos_simulator_arm64",
        "macos_arm64"
    )*/

    fun create(
        name: String,
        targets: List<String>,
        srcDir: java.io.File = project.file("src/$name"),
        outputGroup: String = "main",
        configurationBlock: CompileToBitcode.() -> Unit = {}
    ) {
        val allBitcode = project.tasks.register("all${name.snakeCaseToCamelCase().capitalize()}") {
            it.group = GROUP_NAME
            it.description = "Compiles '$name' to bitcode for all targets"
        }.get()
        val allTaskProviders = targets.map { targetName ->
            val taskName = "${targetName}${name.snakeCaseToCamelCase().capitalize()}"

            val taskProvider = project.tasks.register(
                taskName,
                CompileToBitcode::class.java,
                srcDir, name, targetName, outputGroup
            )
            taskProvider.configure {
                it.group = GROUP_NAME
                it.description = "Compiles '$name' to bitcode for $targetName"
                it.configurationBlock()
            }
            taskProvider
        }
        allBitcode.dependsOn(allTaskProviders)
    }

    companion object {
        private fun String.snakeCaseToCamelCase() =
            split('_').joinToString(separator = "") { it.capitalize() }

        const val GROUP_NAME = "bitcode"
    }
}