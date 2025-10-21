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

import co.touchlab.cklib.gradle.CompileToBitcodePlugin.Companion.PLUGIN_NAME
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

open class CompileToBitcode @Inject constructor(
    srcRoot: File,
    @Input val compileName: String,
    @Input val target: String,
    val enabled: ()->Boolean,
    private val execOperations: ExecOperations,
) : DefaultTask() {

    enum class Language {
        C, CPP, OBJC
    }

    // Compiler args are part of compilerFlags so we don't register them as an input.
    @Input
    val compilerArgs = mutableListOf<String>()
    @Input
    val linkerArgs = mutableListOf<String>()
    @Input
    var excludeFiles: List<String> = listOf(
        "**/*Test.c",
        "**/*TestSupport.c",
        "**/*Test.m",
        "**/*TestSupport.m",
        "**/*Test.cpp",
        "**/*TestSupport.cpp",
        "**/*Test.mm",
        "**/*TestSupport.mm"
    )
    @Input
    var includeFiles: List<String> = listOf(
        "**/*.c",
        "**/*.m",
        "**/*.cpp",
        "**/*.mm"
    )

    // Source files and headers are registered as inputs by the `inputFiles` and `headers` properties.
    @InputFiles
    var srcDirs: FileCollection = project.files(srcRoot.resolve("cpp"))

    @InputFiles
    var headersDirs: FileCollection = srcDirs + project.files(srcRoot.resolve("headers"))

    @Input
    var language = Language.CPP

    @get:Input
    var cStandard = "gnu11"

    @get:Input
    var cppStandard = "c++17"

    @get:Internal
    internal val targetDir: File
        get() {
            return project.buildDir.resolve("$PLUGIN_NAME/$compileName/$target")
        }

    @get:OutputDirectory
    val objDir
        get() = File(targetDir, compileName)

    private val org.jetbrains.kotlin.konan.target.KonanTarget.isMINGW
        get() = this.family == org.jetbrains.kotlin.konan.target.Family.MINGW

    @get:Internal
    val executable
        get() = when (language) {
            Language.C, Language.OBJC -> "clang"
            Language.CPP -> "clang++"
        }

    @get:Input
    val compilerFlags: List<String>
        get() {
            val commonFlags = listOf("-c", "-emit-llvm") + headersDirs.map { "-I$it" }
            val languageFlags = when (language) {
                Language.C ->
                    // Used flags provided by original build of allocator C code.
                    listOf("-std=$cStandard", "-O3", "-Wall", "-Wextra", "-Werror")
                Language.CPP ->
                    listOfNotNull("-std=$cppStandard", "-Werror", "-O2",
                        "-Wall", "-Wextra",
                        "-Wno-unused-parameter"  // False positives with polymorphic functions.
                    )
                Language.OBJC ->
                    // Most of these flags are from Xcode building source. We *may* remove a lot of these eventually, but for now we'll be lazy
                    listOf("-x", "objective-c", "-std=$cStandard", "-fobjc-arc", "-fobjc-weak", //"-framework", "Foundation",// "-mmacosx-version-min=10.6",
                        "-ObjC",
//                        "-fmodules", "-gmodules",
                            "-Wno-missing-field-initializers", "-Wno-missing-prototypes", "-Werror=return-type", "-Wdocumentation", "-Wunreachable-code",
                        "-Wquoted-include-in-framework-header", "-Wno-implicit-atomic-properties", "-Werror=deprecated-objc-isa-usage", "-Wno-objc-interface-ivars",
//
                        "-Werror=objc-root-class", "-Wno-arc-repeated-use-of-weak", "-Wimplicit-retain-self", "-Wduplicate-method-match", "-Wno-missing-braces",
                        "-Wparentheses", "-Wswitch", "-Wunused-function", "-Wno-unused-label", "-Wno-unused-parameter", "-Wunused-variable", "-Wunused-value",
                        "-Wempty-body", "-Wuninitialized", "-Wconditional-uninitialized", "-Wno-unknown-pragmas", "-Wno-shadow", "-Wno-four-char-constants",

                        "-Wno-conversion", "-Wconstant-conversion", "-Wint-conversion", "-Wbool-conversion", "-Wenum-conversion", "-Wno-float-conversion",
                        "-Wnon-literal-null-conversion", "-Wobjc-literal-conversion",
                        "-Wshorten-64-to-32",
                        "-Wpointer-sign", "-Wno-newline-eof", "-Wno-selector",
                        "-Wno-strict-selector-match", "-Wundeclared-selector", "-Wdeprecated-implementations",
//
                        "-fasm-blocks", "-fstrict-aliasing", "-Wprotocol", "-Wdeprecated-declarations", "-g", "-Wno-sign-conversion", "-Winfinite-recursion", "-Wcomma",
                        "-Wblock-capture-autoreleasing", "-Wstrict-prototypes", "-Wno-semicolon-before-method-body", "-Wunguarded-availability", "-fobjc-abi-version=2", "-fobjc-legacy-dispatch",

                    )

                //, "-isysroot", "/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator14.5.sdk"
            }
            return commonFlags + languageFlags + compilerArgs
        }

    @get:SkipWhenEmpty
    @get:InputFiles
    val inputFiles: Iterable<File>
        get() {
            return srcDirs.flatMap { srcDir ->
                project.fileTree(srcDir) {
                    it.include(includeFiles)
                    it.exclude(excludeFiles)
                }.files
            }
        }

    private fun outputFileForInputFile(file: File, extension: String) = objDir.resolve("${file.nameWithoutExtension}.${extension}")
    private fun bitcodeFileForInputFile(file: File) = outputFileForInputFile(file, "bc")

    @get:InputFiles
    protected val headers: Iterable<File>
        get() {
            // Not using clang's -M* flags because there's a problem with our current include system:
            // We allow includes relative to the current directory and also pass -I for each imported module
            // Given file tree:
            // a:
            //  header.hpp
            // b:
            //  impl.cpp
            // Assume module b adds a to its include path.
            // If b/impl.cpp has #include "header.hpp", it'll be included from a/header.hpp. If we add another file
            // header.hpp into b/, the next compilation of b/impl.cpp will include b/header.hpp. -M flags, however,
            // won't generate a dependency on b/header.hpp, so incremental compilation will be broken.
            // TODO: Apart from dependency generation this also makes it awkward to have two files with
            //       the same name (e.g. Utils.h) in directories a/ and b/: For the b/impl.cpp to include a/header.hpp
            //       it needs to have #include "../a/header.hpp"

            val dirs = mutableSetOf<File>()
            // First add dirs with sources, as clang by default adds directory with the source to the include path.
            inputFiles.forEach {
                dirs.add(it.parentFile)
            }
            // Now add manually given header dirs.
            dirs.addAll(headersDirs.files)
            return dirs.flatMap { dir ->
                project.fileTree(dir) {
                    val includePatterns = when (language) {
                        Language.C, Language.OBJC -> arrayOf("**/.h")
                        Language.CPP -> arrayOf("**/*.h", "**/*.hpp")
                    }
                    it.include(*includePatterns)
                }.files
            }
        }

    @get:OutputFile
    val outFile: File
        get() = File(targetDir, "${compileName}.bc")

    @TaskAction
    fun compile() {
        if(!enabled()){
            return
        }
        objDir.mkdirs()

        val plugin = ExecClang(project, execOperations)
        plugin.execKonanClang(target) {
            it.workingDir = objDir
            it.executable = executable
            it.args = compilerFlags + inputFiles.map { it.absolutePath }
        }

        execOperations.exec {

//            val llvmDir = platformManager.hostPlatform.llvmHome//"abc"///*platformManager.hostPlatform.llvmHome*/project.findProperty("llvmDir")
            val llvmDir = project.llvmHome
            it.executable = "$llvmDir/bin/llvm-link"
            it.args = listOf("-o", outFile.absolutePath) + linkerArgs +
                    inputFiles.map {
                        bitcodeFileForInputFile(it).absolutePath
                    }
        }
    }
}