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

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import java.io.File

class ExecClang(private val project: Project) {

    private val platformManager = project.platformManager

    private fun clangArgsForCppRuntime(target: org.jetbrains.kotlin.konan.target.KonanTarget): List<String> {
        return platformManager.clangArgsForKonanSourcesForTarget(target)
    }

    private fun clangArgsForCppRuntime(targetName: String?): List<String> {
        val target = platformManager.targetForName(targetName)
        return clangArgsForCppRuntime(target)
    }

    private fun resolveExecutable(executableOrNull: String?): String {
        val executable = executableOrNull ?: "clang"

        if (listOf("clang", "clang++").contains(executable)) {
            val llvmDir = project.llvmHome
            return "${llvmDir}/bin/$executable"
        } else {
            throw GradleException("unsupported clang executable: $executable")
        }
    }

    // The konan ones invoke clang with konan provided sysroots.
    // So they require a target or assume it to be the host.
    // The target can be specified as KonanTarget or as a
    // (nullable, which means host) target name.

    fun execKonanClang(target: String?, action: Action<in ExecSpec>): ExecResult {
        return this.execClang(clangArgsForCppRuntime(target), action)
    }

    private fun execClang(defaultArgs: List<String>, action: Action<in ExecSpec>): ExecResult {
        val extendedAction = Action<ExecSpec> {
            action.execute(it)
            it.executable = resolveExecutable(it.executable)

//            val hostPlatform = platformManager.hostPlatform//project.findProperty("hostPlatform") as org.jetbrains.kotlin.konan.target.Platform
            it.environment["PATH"] = project.files(platformManager.hostPlatform_clang_clangPaths).asPath +
                    File.pathSeparator + it.environment["PATH"]
            it.args = it.args + defaultArgs
        }
        return project.exec(extendedAction)
    }
}