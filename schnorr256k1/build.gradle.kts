import java.io.File
import java.util.Properties
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "com.vitorpamplona.schnorr256k1"
version = "1.0.4"

val libschnorrDir = rootProject.projectDir.resolve("libschnorr256k1")

fun resolveCmakeExecutable(): String {
    val exeName = if (OperatingSystem.current().isWindows) "cmake.exe" else "cmake"

    System.getenv("PATH")?.split(File.pathSeparator).orEmpty().forEach { dir ->
        val candidate = File(dir, exeName)
        if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
    }

    val extraPaths = listOf(
        "/opt/homebrew/bin",
        "/usr/local/bin",
        "/usr/bin",
        "/opt/local/bin",
    )
    extraPaths.forEach { dir ->
        val candidate = File(dir, exeName)
        if (candidate.isFile && candidate.canExecute()) return candidate.absolutePath
    }

    val sdkCmakeRoot = resolveAndroidSdkDir()?.resolve("cmake")
    if (sdkCmakeRoot?.isDirectory == true) {
        val newest = sdkCmakeRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }
        val candidate = newest?.resolve("bin/$exeName")
        if (candidate?.isFile == true && candidate.canExecute()) return candidate.absolutePath
    }

    return exeName
}

val cmakeExecutable: String by lazy { resolveCmakeExecutable() }

kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // ==================== JVM ====================
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    // ==================== Android ====================
    android {
        namespace = "com.vitorpamplona.schnorr256k1"
        compileSdk = 37
        minSdk = 26

        withHostTest {}

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_21)
                }
            }
        }
    }

    // ==================== Native ====================
    val hostOs = OperatingSystem.current()
    if (hostOs.isLinux) {
        linuxX64()
    }
    if (hostOs.isMacOsX) {
        macosArm64()
        iosArm64()
        iosX64()
        iosSimulatorArm64()
    }

    // ==================== JS ====================
    js(IR) {
        browser {
            testTask {
                useMocha()
            }
        }
        nodejs()
    }

    // ==================== Wasm ====================
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            // Tests disabled: Kotlin/Wasm 2.1 lacks an eager-init hook to
            // register a Mocha `before(done)` async bridge setup before the
            // test runner starts. Re-enable once a pre-test hook is available.
            testTask { enabled = false }
        }
        nodejs {
            testTask { enabled = false }
        }
    }

    // ==================== cinterop for all native targets ====================
    targets.withType<KotlinNativeTarget> {
        val targetName = this.name
        compilations.getByName("main") {
            cinterops {
                val schnorr256k1 by creating {
                    defFile(project.file("src/nativeInterop/cinterop/schnorr256k1.def"))
                    includeDirs(libschnorrDir.resolve("include"))
                    extraOpts(
                        "-libraryPath",
                        layout.buildDirectory.dir("native/$targetName").get().asFile.absolutePath,
                    )
                }
            }
        }
    }

    // ==================== Source sets ====================
    sourceSets {
        commonMain {}
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        jvmMain {
            dependencies {
                // Bundle all four JNI artifacts so consumers get the right
                // native binary out of the box; NativeLoader picks one at
                // runtime by os.name/os.arch.
                runtimeOnly(project(":jni-jvm-linux-x86_64"))
                runtimeOnly(project(":jni-jvm-linux-aarch64"))
                runtimeOnly(project(":jni-jvm-darwin-x86_64"))
                runtimeOnly(project(":jni-jvm-darwin-aarch64"))
            }
        }
        androidMain {}
        nativeMain {}
        jsMain {}
        val wasmJsMain by getting {
            dependencies {
                implementation(libs.kotlinx.browser)
            }
        }
    }
}

// ==================== Desktop JVM: JNI library is provided by ====================
// :jni-jvm-{linux,darwin}-{x86_64,aarch64} subprojects. Their JARs ship
// libschnorr256k1_jni as a classpath resource that NativeLoader extracts at
// runtime — so :jvmTest needs no java.library.path tweak.
//
// The Android host-test target (Android unit tests running on JVM) calls
// System.loadLibrary directly from Schnorr256k1.android.kt, so it still needs
// a flat directory containing libschnorr256k1_jni.{so,dylib}. We reuse the
// host's :jni-jvm-* subproject build output for that.
val hostJniProjectName = run {
    val arch = System.getProperty("os.arch").lowercase()
    val os = OperatingSystem.current()
    val archClassifier = when (arch) {
        "amd64", "x86_64", "x64" -> "x86_64"
        "aarch64", "arm64" -> "aarch64"
        else -> null
    }
    val osClassifier = when {
        os.isLinux -> "linux"
        os.isMacOsX -> "darwin"
        else -> null
    }
    if (osClassifier != null && archClassifier != null) {
        ":jni-jvm-$osClassifier-$archClassifier"
    } else {
        null
    }
}

if (hostJniProjectName != null) {
    val hostJniProject = project(hostJniProjectName)
    val hostNativeDir = hostJniProject.layout.buildDirectory.dir(
        "jni-native/${hostJniProjectName.removePrefix(":jni-jvm-")}",
    )

    tasks.withType<Test>().configureEach {
        if (name.contains("AndroidHostTest", ignoreCase = true)) {
            dependsOn("$hostJniProjectName:buildJniLibrary")
            systemProperty("java.library.path", hostNativeDir.get().asFile.absolutePath)
        }
    }
}

// ==================== Native: build static C library for each target ====================
fun registerNativeBuildTasks(targetName: String, cmakeFlags: List<String> = emptyList()) {
    val outputDir = layout.buildDirectory.dir("native/$targetName").get().asFile
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-$targetName").get().asFile

    val configureTask = tasks.register<Exec>("configureNative_$targetName") {
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.mkdirs()
        }
        workingDir = cmakeBuildDir
        commandLine(
            listOf(
                cmakeExecutable, libschnorrDir.absolutePath,
                "-DCMAKE_BUILD_TYPE=Release",
                "-DBUILD_TESTS=OFF",
                "-DBUILD_BENCH=OFF",
                "-DCMAKE_ARCHIVE_OUTPUT_DIRECTORY=${outputDir.absolutePath}",
            ) + cmakeFlags,
        )
    }

    val buildTask = tasks.register<Exec>("buildNative_$targetName") {
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
    }

    tasks.matching {
        it.name.startsWith("cinteropSchnorr256k1") &&
            it.name.endsWith(targetName.replaceFirstChar { c -> c.uppercase() })
    }.configureEach {
        dependsOn(buildTask)
    }
}

val os = OperatingSystem.current()
if (os.isLinux) {
    registerNativeBuildTasks("linuxX64")
}
if (os.isMacOsX) {
    registerNativeBuildTasks(
        "macosArm64",
        listOf(
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
        ),
    )
    registerNativeBuildTasks(
        "iosArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
        ),
    )
    registerNativeBuildTasks(
        "iosX64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
            "-DCMAKE_OSX_ARCHITECTURES=x86_64",
            "-DCMAKE_OSX_SYSROOT=iphonesimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
        ),
    )
    registerNativeBuildTasks(
        "iosSimulatorArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_SYSTEM_PROCESSOR=arm64",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=iphonesimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
        ),
    )
}

// ==================== Android: build JNI shared library per ABI ====================
val androidJniAbis = listOf("arm64-v8a", "x86_64")
val androidApiLevel = 26

val pinnedAndroidNdkVersion = "30.0.14904198"

fun resolveAndroidSdkDir(): java.io.File? {
    listOf("ANDROID_HOME", "ANDROID_SDK_ROOT").forEach { key ->
        System.getenv(key)?.takeIf { it.isNotBlank() }?.let {
            val f = file(it)
            if (f.isDirectory) return f
        }
    }
    val localProps = rootProject.file("local.properties")
    if (localProps.isFile) {
        val props = Properties().apply { localProps.inputStream().use { load(it) } }
        props.getProperty("sdk.dir")?.takeIf { it.isNotBlank() }?.let {
            val f = file(it)
            if (f.isDirectory) return f
        }
    }
    return null
}

fun resolveAndroidNdkDir(): java.io.File? {
    listOf("ANDROID_NDK_HOME", "ANDROID_NDK_ROOT", "NDK_HOME").forEach { key ->
        System.getenv(key)?.takeIf { it.isNotBlank() }?.let {
            val f = file(it)
            if (f.isDirectory) return f
        }
    }
    val sdk = resolveAndroidSdkDir() ?: return null
    val ndkParent = sdk.resolve("ndk")
    if (!ndkParent.isDirectory) return null
    val pinned = ndkParent.resolve(pinnedAndroidNdkVersion)
    if (pinned.isDirectory) return pinned
    return ndkParent.listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.name }
}

val androidJniLibsDir = layout.buildDirectory.dir("jniLibs")
val resolvedAndroidNdk = resolveAndroidNdkDir()
val androidNdkToolchain = resolvedAndroidNdk?.resolve("build/cmake/android.toolchain.cmake")

val buildAndroidJniLibs by tasks.registering {
    group = "build"
    description = "Builds the JNI shared library for all Android ABIs."
}

androidJniAbis.forEach { abi ->
    val outputDir = layout.buildDirectory.dir("jniLibs/$abi")
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-android-$abi").get().asFile

    val configureTask = tasks.register<Exec>("configureAndroidJni_$abi") {
        onlyIf { androidNdkToolchain?.isFile == true }
        doFirst {
            cmakeBuildDir.mkdirs()
            outputDir.get().asFile.mkdirs()
        }
        workingDir = cmakeBuildDir
        commandLine(
            cmakeExecutable, "${rootProject.projectDir}/jni",
            "-DCMAKE_BUILD_TYPE=Release",
            "-DCMAKE_TOOLCHAIN_FILE=${androidNdkToolchain?.absolutePath ?: ""}",
            "-DANDROID_ABI=$abi",
            "-DANDROID_PLATFORM=android-$androidApiLevel",
            "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${outputDir.get().asFile.absolutePath}",
        )
    }

    val buildTask = tasks.register<Exec>("buildAndroidJni_$abi") {
        onlyIf { androidNdkToolchain?.isFile == true }
        dependsOn(configureTask)
        workingDir = cmakeBuildDir
        commandLine(cmakeExecutable, "--build", ".", "--config", "Release")
    }

    buildAndroidJniLibs.configure { dependsOn(buildTask) }
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addStaticSourceDirectory(
            androidJniLibsDir.get().asFile.absolutePath,
        )
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.contains("JniLibFolders") }
    .configureEach { dependsOn(buildAndroidJniLibs) }

// ==================== Publishing ====================
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = "schnorr256k1-kmp",
        version = version.toString(),
    )

    pom {
        name.set("schnorr256k1-kmp")
        description.set(
            "Kotlin Multiplatform bindings for libschnorr256k1 — a high-performance " +
                "secp256k1 library optimized for Nostr/BIP-340 workflows.",
        )
        url.set("https://github.com/vitorpamplona/libschnorr256k1-kmp")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("vitorpamplona")
                name.set("Vitor Pamplona")
                url.set("https://github.com/vitorpamplona")
            }
        }

        scm {
            url.set("https://github.com/vitorpamplona/libschnorr256k1-kmp")
            connection.set("scm:git:git://github.com/vitorpamplona/libschnorr256k1-kmp.git")
            developerConnection.set("scm:git:ssh://git@github.com/vitorpamplona/libschnorr256k1-kmp.git")
        }

        issueManagement {
            system.set("GitHub")
            url.set("https://github.com/vitorpamplona/libschnorr256k1-kmp/issues")
        }
    }
}
