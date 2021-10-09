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
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.TargetSupportException
import javax.inject.Inject

open class CKlibGradleExtension @Inject constructor(val project: Project){
    var konanHome: String = "${System.getProperty("user.home")}/.konan/kotlin-native-prebuilt-${simpleOsName}-x86_64-${GradleValues.KOTLIN_VERSION}"
    var llvmHome: String = "${System.getProperty("user.home")}/.konan/dependencies/${llvmName}"
}

internal val Project.platformManager: PlatformManager
    get() {
        val cklibExtension = extensions.getByType(CKlibGradleExtension::class.java)
        return PlatformManager(Distribution(cklibExtension.konanHome), cklibExtension.konanHome)
    }

internal val Project.llvmHome: String
    get() = extensions.getByType(CKlibGradleExtension::class.java).llvmHome

internal val simpleOsName: String
    get() {
        val hostOs = HostManager.hostOs()
        return if (hostOs == "osx") "macos" else hostOs
    }

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