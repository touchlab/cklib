# C Klib

CKlib is a gradle plugin that will build and package C/C++/Objective-C code for Kotlin/Native.

# Usage

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

See example in [Kermit](https://github.com/touchlab/Kermit/blob/main/kermit-crashlytics-test/build.gradle.kts#L69)

# Note

The main Kotlin project has changed how locally embedded C-like code is included in libraries. Use 
this project if you'd like, but outside of private projects we won't really be supporting it much.

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