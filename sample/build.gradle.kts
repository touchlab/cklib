/*
 * Copyright (c) 2021 Touchlab
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

import co.touchlab.cklib.gradle.CompileToBitcode

plugins {
    kotlin("multiplatform") version "2.0.0"
    id("co.touchlab.cklib")
}

kotlin {
    val nativeTargets = listOf(
        macosArm64(),
        macosX64(),
        iosArm64(),
        iosSimulatorArm64(),
        linuxX64(),
    )

    nativeTargets.forEach { target ->
        target.compilations.getByName("main").cinterops.create("monocypher") {
            defFile(project.file("src/nativeInterop/cinterop/monocypher.def"))
            // cinterop only needs the headers. The compiled code arrives as bitcode from cklib,
            // which is why the .def declares no staticLibraries or libraryPaths.
            includeDirs(project.file("src/monocypher/headers"))
        }
    }

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

cklib {
    config.kotlinVersion = "2.0.0"

    create("monocypher", srcDir = file("src/monocypher")) {
        language = CompileToBitcode.Language.C

        // Both default to a `cpp` subdirectory (headersDirs additionally to `headers`) even for
        // Language.C, so point them at the real layout instead of naming a C folder "cpp".
        srcDirs = files("src/monocypher/c")
        headersDirs = files("src/monocypher/headers")

        // No compilerArgs. Language.C compiles with a hardcoded
        // `-std=gnu11 -O3 -Wall -Wextra -Werror`, and both Monocypher 4.0.3 and secure_random.c
        // build clean under it, so no -Wno-error= escape hatch is needed.
        // compilerArgs.addAll(
        //     listOf(
        //     )
        // )
    }
}
