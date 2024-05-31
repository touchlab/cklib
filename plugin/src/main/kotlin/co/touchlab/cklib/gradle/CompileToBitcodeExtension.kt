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
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import javax.inject.Inject

open class CompileToBitcodeExtension @Inject constructor(val project: Project) {

    val config = CKlibGradleExtension(project)

    fun create(
        name: String,
        srcDir: java.io.File = project.file("src/$name"),
        compilations: List<String> = listOf("main"),
        configurationBlock: CompileToBitcode.() -> Unit = {}
    ) {
        val kmpExt = project.kmpExt

        val allBitcode = project.tasks.register("all${name.snakeCaseToCamelCase().capitalize()}") {
            it.group = GROUP_NAME
            it.description = "Compiles '$name' to bitcode for all targets"
        }.get()

        val allTaskProviders = kmpExt.kotlinNativeTargets.map { knTarget ->

            val compileKotlinTask = project.tasks.getByPath("compileKotlin${knTarget.name.capitalize()}")

            val taskName = "${knTarget.name}${name.snakeCaseToCamelCase().capitalize()}"

            val taskProvider = project.tasks.register(
                taskName,
                CompileToBitcode::class.java,
                srcDir, name, knTarget.konanTarget.name, { project.platformManager.isEnabled(knTarget.konanTarget) }
            )

            //tasks.getByName("compileKotlin${targetName.capitalize()}").dependsOn("${it.second}Quickjs")

            compileKotlinTask
                .dependsOn(taskName)

            taskProvider.configure { compileToBitcodeTask ->
                compileToBitcodeTask.group = GROUP_NAME
                compileToBitcodeTask.description = "Compiles '$name' to bitcode for ${knTarget.name}"

                compilations.forEach { compilation ->
                    val knCompilation = knTarget.compilations.getByName(compilation)

                    knCompilation.kotlinOptions.freeCompilerArgs +=
                        listOf("-native-library", compileToBitcodeTask.outFile.absolutePath)
                }

                compileToBitcodeTask.configurationBlock()
            }

            taskProvider
        }
        allBitcode.dependsOn(allTaskProviders)
    }

    companion object {
        private fun String.snakeCaseToCamelCase() =
            split('_').joinToString(separator = "") { it.capitalize() }

        const val GROUP_NAME = CompileToBitcodePlugin.PLUGIN_NAME
    }
}

private val KotlinMultiplatformExtension.kotlinNativeTargets: Collection<KotlinNativeTarget>
    get() = targets.withType(KotlinNativeTarget::class.java)

private val Project.kmpExt: KotlinMultiplatformExtension
    get() = extensions.getByType(KotlinMultiplatformExtension::class.java)