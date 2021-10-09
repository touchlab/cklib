/*
 * Copyright (c) 2021 Touchlab
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package co.touchlab.cklib.gradle.reflection

import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File
import java.net.URLClassLoader

/**
 * Use reflection to grab what we want from the konan distribution
 */
class PlatformManager(dist: Distribution, konanHome:String) {
    private val kotlinNativeJar = "${konanHome}/konan/lib/kotlin-native.jar"
    private val pmClass:Class<*>
    private val blindDelegate:Any

    init {
        val child = URLClassLoader(arrayOf(File(kotlinNativeJar).toURL()), this.javaClass.classLoader)
        pmClass = Class.forName("org.jetbrains.kotlin.konan.target.PlatformManager", true, child)
        blindDelegate = pmClass.declaredConstructors.find {
            it.parameters.size == 2 && it.parameters[0].type == Distribution::class.java
        }!!.newInstance(dist, false)
    }

    //targetManager(targetName).target
    fun targetForName(name: String?): KonanTarget {
        val targetManagerMethod = pmClass.superclass.getDeclaredMethod("targetManager", String::class.java)
        val targetManagerInstance = targetManagerMethod.invoke(blindDelegate, name)
        val getTargetMethod = targetManagerInstance.javaClass.getDeclaredMethod("getTarget")

        return getTargetMethod.invoke(targetManagerInstance) as KonanTarget
    }

    //platform(target).clang.clangArgs.toList()
    fun clangArgsForTarget(target: KonanTarget): List<String> = clangBagOfStrings(target, "getClangArgs")

    //platform(target).clang.clangArgsForKonanSources.asList()
    fun clangArgsForKonanSourcesForTarget(target: KonanTarget): List<String> = clangBagOfStrings(target, "getClangArgsForKonanSources")

    private fun clangBagOfStrings(target: KonanTarget, methodName: String): List<String>{
        val platformMethod = pmClass.getDeclaredMethod("platform", KonanTarget::class.java)
        val platformInstance = platformMethod.invoke(blindDelegate, target)
        val clangArgsInstance = platformInstance.javaClass.getDeclaredMethod("getClang").invoke(platformInstance)
        return (clangArgsInstance.javaClass.getDeclaredMethod(methodName).invoke(clangArgsInstance) as Array<String>).toList()
    }

    //platformManager.hostPlatform.clang.clangPaths
    val hostPlatform_clang_clangPaths: List<String>
        get() {
            val platformMethod = pmClass.getDeclaredMethod("getHostPlatform")
            val platformInstance = platformMethod.invoke(blindDelegate)
            val clangArgsInstance = platformInstance.javaClass.getDeclaredMethod("getClang").invoke(platformInstance)
            return clangArgsInstance.javaClass.getDeclaredMethod("getClangPaths").invoke(clangArgsInstance) as List<String>
        }
}