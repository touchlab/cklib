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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class CompileToBitcodePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.create(EXTENSION_NAME, CompileToBitcodeExtension::class.java, target)

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

    companion object {
        const val EXTENSION_NAME = "cklib"
        const val PLUGIN_NAME = "cklib"
    }
}
