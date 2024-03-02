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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*

class CompileToBitcodePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.create(EXTENSION_NAME, CompileToBitcodeExtension::class.java, target)
        downloadIfNeeded(target)

        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform"){ appliedPlugin ->
            extensions.getByType(KotlinMultiplatformExtension::class.java)
        }
//        target.pluginManager.apply(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMultiplatformPlugin::class.java)

        Unit
        /*afterEvaluate {
            // TODO: Support providers (https://docs.gradle.org/current/userguide/lazy_configuration.html)
            //       in database tasks and create them along with corresponding compile tasks (not in afterEvaluate).
            org.jetbrains.kotlin.createCompilationDatabasesFromCompileToBitcodeTasks(
                project,
                COMPILATION_DATABASE_TASK_NAME
            )
        }*/
    }

    //This is pretty hacky, but the process changed in 1.6.0. We'll probably just split off and do our own thing
    //going forward, but need to be able to build for the next few weeks.
    private fun downloadIfNeeded(target: Project) {
        val cklibDir = File(defaultCklibDir)
        val localFile = File(cklibDir, llvmName)
        val clangExists = localFile.exists()
        if(!clangExists){
            target.logger.info("cklib downloading dependencies (may take a while...)")
            cklibDir.mkdirs()
            val tempFileName = UUID.randomUUID().toString()
            val tempFileNameWithExtension = "${tempFileName}.tar.gz"
            val tempDl = File(cklibDir, tempFileNameWithExtension)

            try {
                val fos = FileOutputStream(tempDl)

                val inp = BufferedInputStream(URL("https://download.jetbrains.com/kotlin/native/${llvmName}.${archiveType}").openStream())
                inp.copyTo(fos)
                fos.close()
                inp.close()

                target.exec {
                    it.workingDir = cklibDir.absoluteFile
                    it.executable = "tar"
                    it.args = listOf("-xf", tempFileNameWithExtension)
                }
            } finally {
                tempDl.delete()
            }
        }
    }

    companion object {
        const val EXTENSION_NAME = "cklib"
        const val PLUGIN_NAME = "cklib"
    }
}
