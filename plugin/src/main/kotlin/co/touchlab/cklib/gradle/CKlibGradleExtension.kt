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
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.PlatformManager
import javax.inject.Inject

open class CKlibGradleExtension @Inject constructor(val project: Project){
    var konanHome: String = "${System.getProperty("user.home")}/.konan/kotlin-native-prebuilt-macos-x86_64-1.5.30"
    var llvmHome: String = "${System.getProperty("user.home")}/.konan/dependencies/clang-llvm-apple-8.0.0-darwin-macos"
}

internal val Project.platformManager: PlatformManager
    get() {
        val cklibExtension = extensions.getByType(CKlibGradleExtension::class.java)
        return PlatformManager(Distribution(cklibExtension.konanHome))
    }

internal val Project.llvmHome: String
    get() = extensions.getByType(CKlibGradleExtension::class.java).llvmHome