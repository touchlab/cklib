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

import co.touchlab.cklib.gradle.reflection.PlatformManager
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetSupportException
import javax.inject.Inject

open class CKlibGradleExtension @Inject constructor(val project: Project) {
    private var _konanHome: String? = null
    private var _llvmHome: String? = null
    private var _llvmDir: String? = null

    var kotlinVersion: String? = null
    var arch: String = hostArch

    private fun makeKonanHome(): String {
        if (kotlinVersion == null) {
            throw GradleException("CKLib 'config.kotlinVersion' required. See https://github.com/touchlab/cklib")
        }
        return "${System.getProperty("user.home")}/.konan/kotlin-native-prebuilt-${simpleOsName}-${arch}-${kotlinVersion}"
    }

    var konanHome: String
        get() = _konanHome ?: makeKonanHome()
        set(value) {
            _konanHome = value
        }

    var llvmHome: String
        get() = _llvmHome ?: "${System.getProperty("user.home")}/.konan/dependencies/${llvmDir}"
        set(value) {
            _llvmHome = value
        }

    var llvmDir: String
        get() = _llvmDir ?: llvmName
        set(value) {
            _llvmDir = value
        }
}

internal val Project.platformManager: PlatformManager
    get() {
        val cklibExtension = extensions.getByType(CompileToBitcodeExtension::class.java)
        return PlatformManager(Distribution(cklibExtension.config.konanHome), cklibExtension.config.konanHome)
    }

internal val Project.llvmHome: String
    get() = extensions.getByType(CompileToBitcodeExtension::class.java).config.llvmHome

internal val simpleOsName: String
    get() {
        val hostOs = HostManager.hostOs()
        return if (hostOs == "osx") "macos" else hostOs
    }

internal val hostArch: String
    get() = HostManager.hostArch()

internal val osName: String
    get() {
        val javaOsName = System.getProperty("os.name")
        return when {
            javaOsName == "Mac OS X" -> "osx"
            javaOsName == "Linux" -> "linux"
            javaOsName.startsWith("Windows") -> "windows"
            else -> throw TargetSupportException("Unknown operating system: $javaOsName")
        }
    }

internal val llvmName: String
    get() {
        return when (osName) {
            "osx" -> llvm_macos_x64
            "linux" -> llvm_linux_x64
            "windows" -> llvm_mingw_x64
            else -> throw TargetSupportException("Unknown operating system: $osName")
        }
    }

internal val llvm_linux_x64 = "clang-llvm-8.0.0-linux-x86-64"
internal val llvm_mingw_x64 = "msys2-mingw-w64-x86_64-clang-llvm-lld-compiler_rt-8.0.1"
internal val llvm_macos_x64 = "clang-llvm-apple-8.0.0-darwin-macos"
internal val llvm_macos_arm64 = "clang-llvm-apple-8.0.0-darwin-macos-aarch64"