# C Klib sample: passphrase-encrypted notes with Monocypher

A sample for C Klib that packages the C library [Monocypher](https://monocypher.org). It's a simple passphrase-encrypted note implementation.

```kotlin
val sealed: ByteArray = SecureNote.seal("hunter2", "meet me at the docks at midnight")
val note: String? = SecureNote.open("hunter2", sealed)   // null if the passphrase is wrong
```

C Klib compiles C to LLVM bitcode; cinterop reads the same headers and generates the Kotlin
declarations; the Kotlin/Native compiler links the two together.

## Running the sample

From the repository root, using the root wrapper:
```bash
./gradlew -p sample build            # compile every declared target
./gradlew -p sample macosArm64Test   # the host-runnable test suite
./gradlew -p sample allMonocypher    # just the bitcode, for every target
```

`sample/settings.gradle.kts` uses `pluginManagement { includeBuild("..") }`, so the plugin is built
from this working tree rather than downloaded. No `version` on the plugin id, and no
`include(":sample")` in the root build.

> **Note:** The first run downloads an LLVM toolchain into `~/.cklib` (about 1.6 GB) and Kotlin/Native into
`~/.konan`. 

## Notes

* **`srcDirs` defaults to `srcRoot/cpp`, even for `Language.C`.** `headersDirs` defaults to
`srcDirs + srcRoot/headers`. If you want your C in a directory not named `cpp`, override both:

```kotlin
create("monocypher", srcDir = file("src/monocypher")) {
    language = CompileToBitcode.Language.C
    srcDirs = files("src/monocypher/c")
    headersDirs = files("src/monocypher/headers")
}
```

* **`mingwX64`** is deliberately not declared: `secure_random.c` has no Windows branch, and adding one
means `BCryptGenRandom` plus linking `bcrypt.lib`.

* **`Language.C` compiles with `-std=gnu11 -O3 -Wall -Wextra -Werror`, hardcoded.** The only escape
hatch is `compilerArgs`, which is appended after those flags, so `-Wno-error=<specific>` works.
This sample needs **no suppressions at all**: Monocypher 4.0.3 and `secure_random.c` both build
clean. Vendored code that is not warning-clean will stop the build dead, so check before you commit
to a library.

* **Shared `nativeMain` + cinterop needs `kotlin.mpp.enableCInteropCommonization=true`.** Without it,
per-target tasks like `compileKotlinMacosArm64` and `macosArm64Test` work fine, but
`compileNativeMainKotlinMetadata` — which `build` runs — fails with `Unresolved reference
'cinterop'` for every binding. It is in `gradle.properties`.

* **`config.kotlinVersion` must match the Kotlin plugin version.** CKlib resolves the toolchain at
`~/.konan/kotlin-native-prebuilt-<os>-<arch>-<kotlinVersion>` and fails with a bare
`InvocationTargetException` if that directory is missing. On a machine with no Kotlin/Native
installed yet, the bitcode task can therefore fail before anything has had a chance to download it;
running any Kotlin/Native task first (`./gradlew -p sample cinteropMonocypherMacosArm64`) fetches
the distribution and unblocks it.
