# C Klib

CKlib is a gradle plugin that will build and package C/C++/Objective-C code for Kotlin/Native.

> **Note:** The main Kotlin project has changed how locally embedded C-like code is included in libraries. Use this project if you'd like, but outside of private projects we won't really be supporting it much.
## Usage

Add gradle plugins

```kotlin
plugins {
    kotlin("multiplatform")
    id("co.touchlab.cklib")
}
```

Add Kotlin version and define some C-like source:

```kotlin
cklib {
    config.kotlinVersion = KOTLIN_VERSION
    create("objcsample") {
        language = Language.OBJC
    }
}
```

## Configuration

Configuring custom C Standard and llvmHome.
```kotlin
cklib {
    config.kotlinVersion = KOTLIN_VERSION
    config.llvmHome = "Absolute path to your local llvmHome version"
    create("csample") {
        language = Language.C
        cStandard = "gnu23"
    }
}
```

- Default `cStandard`: `gnu11`
- Default `cppStandard`: `c++17`

## Examples

You can find a [tutorial](https://hackernoon.com/how-to-extend-a-kmm-shared-module-with-cc-code) with a [GitHub Sample](https://github.com/ttypic/kmm-embedded-c) to get a brief understanding of how the library works.

Additionally you can see multiple examples of C Klib in use here:
1. [zstd-kmp](https://github.com/square/zstd-kmp) - Packages [ztsd](https://github.com/facebook/zstd), a fast real-time compression algorithm.
1. [Zipline](https://github.com/cashapp/zipline) - Packages [QuickJS](https://bellard.org/quickjs/), a small and embeddable Javascript engine.



License
=======

    Copyright 2021 Touchlab, Inc.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.