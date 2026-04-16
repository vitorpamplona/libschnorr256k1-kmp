import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "com.vitorpamplona"
version = "1.0.0"

val libschnorrDir = rootProject.projectDir.resolve("libschnorr256k1")

kotlin {
    applyDefaultHierarchyTemplate()

    // ==================== JVM ====================
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
            systemProperty(
                "java.library.path",
                layout.buildDirectory.dir("native/jvm").get().asFile.absolutePath,
            )
        }
    }

    // ==================== Android ====================
    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        publishLibraryVariants("release")
    }

    // ==================== Native ====================
    linuxX64()
    macosX64()
    macosArm64()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

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
        browser()
        nodejs()
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
        jvmMain {}
        androidMain {}
        nativeMain {}
        jsMain {}
        val wasmJsMain by getting
    }
}

// ==================== Android ====================
android {
    namespace = "com.vitorpamplona.schnorr256k1"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("${rootProject.projectDir}/jni/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ==================== Desktop JVM: build JNI shared library ====================
val buildNativeJvm by tasks.registering(Exec::class) {
    val nativeDir = layout.buildDirectory.dir("native/jvm").get().asFile
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-jvm").get().asFile

    doFirst {
        cmakeBuildDir.mkdirs()
        nativeDir.mkdirs()
    }

    workingDir = cmakeBuildDir

    val os = OperatingSystem.current()
    val javaHome = System.getProperty("java.home") ?: System.getenv("JAVA_HOME") ?: ""
    val jniInclude = if (javaHome.isNotEmpty()) "$javaHome/include" else ""
    val jniPlatformInclude = when {
        os.isLinux -> "$jniInclude/linux"
        os.isMacOsX -> "$jniInclude/darwin"
        os.isWindows -> "$jniInclude/win32"
        else -> ""
    }

    commandLine(
        "cmake",
        "${rootProject.projectDir}/jni",
        "-DCMAKE_BUILD_TYPE=Release",
        "-DJNI_INCLUDE_DIR=$jniInclude",
        "-DJNI_INCLUDE_DIR_PLATFORM=$jniPlatformInclude",
        "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${nativeDir.absolutePath}",
    )
}

val compileNativeJvm by tasks.registering(Exec::class) {
    dependsOn(buildNativeJvm)
    val cmakeBuildDir = layout.buildDirectory.dir("cmake-jvm").get().asFile
    workingDir = cmakeBuildDir
    commandLine("cmake", "--build", ".", "--config", "Release")
}

tasks.named("jvmProcessResources") { dependsOn(compileNativeJvm) }
tasks.named("jvmTest") { dependsOn(compileNativeJvm) }

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
                "cmake", libschnorrDir.absolutePath,
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
        commandLine("cmake", "--build", ".", "--config", "Release")
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
    registerNativeBuildTasks("macosX64", listOf("-DCMAKE_OSX_ARCHITECTURES=x86_64"))
    registerNativeBuildTasks("macosArm64", listOf("-DCMAKE_OSX_ARCHITECTURES=arm64"))
    registerNativeBuildTasks(
        "iosArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
        ),
    )
    registerNativeBuildTasks(
        "iosX64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_OSX_ARCHITECTURES=x86_64",
            "-DCMAKE_OSX_SYSROOT=iphonesimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
        ),
    )
    registerNativeBuildTasks(
        "iosSimulatorArm64",
        listOf(
            "-DCMAKE_SYSTEM_NAME=iOS",
            "-DCMAKE_OSX_ARCHITECTURES=arm64",
            "-DCMAKE_OSX_SYSROOT=iphonesimulator",
            "-DCMAKE_OSX_DEPLOYMENT_TARGET=13.0",
        ),
    )
}

// ==================== Publishing ====================
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.vitorpamplona"
            artifactId = "schnorr256k1-kmp"
            version = project.version.toString()
        }
    }
}
