import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    `maven-publish`
}

group = "com.vitorpamplona"
version = "1.0.0"

kotlin {
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

    androidTarget {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        publishLibraryVariants("release")
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        val jvmMain by getting
        val jvmTest by getting
        val androidMain by getting
    }
}

android {
    namespace = "com.vitorpamplona.schnorr256k1"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
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

// Build JNI shared library for JVM (desktop)
val buildNativeJvm by tasks.registering(Exec::class) {
    val nativeDir = layout.buildDirectory.dir("native/jvm").get().asFile
    val buildDir = layout.buildDirectory.dir("cmake-jvm").get().asFile

    doFirst {
        buildDir.mkdirs()
        nativeDir.mkdirs()
    }

    workingDir = buildDir

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
    val buildDir = layout.buildDirectory.dir("cmake-jvm").get().asFile
    workingDir = buildDir
    commandLine("cmake", "--build", ".", "--config", "Release")
}

tasks.named("jvmProcessResources") {
    dependsOn(compileNativeJvm)
}

tasks.named("jvmTest") {
    dependsOn(compileNativeJvm)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.vitorpamplona"
            artifactId = "schnorr256k1-kmp"
            version = project.version.toString()
        }
    }
}
