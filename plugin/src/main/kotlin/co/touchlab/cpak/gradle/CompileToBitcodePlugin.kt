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

import org.gradle.api.Plugin
import org.gradle.api.Project

class CompileToBitcodePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.create(EXTENSION_NAME, CompileToBitcodeExtension::class.java, target)
        extensions.create(CPAK_EXTENSION_NAME, CpakGradleExtension::class.java, target)

        afterEvaluate {
            // TODO: Support providers (https://docs.gradle.org/current/userguide/lazy_configuration.html)
            //       in database tasks and create them along with corresponding compile tasks (not in afterEvaluate).
            org.jetbrains.kotlin.createCompilationDatabasesFromCompileToBitcodeTasks(
                project,
                COMPILATION_DATABASE_TASK_NAME
            )
        }
    }

    companion object {
        const val EXTENSION_NAME = "bitcode"
        const val CPAK_EXTENSION_NAME = "cpak"
        const val COMPILATION_DATABASE_TASK_NAME = "CompilationDatabase"
    }
}
