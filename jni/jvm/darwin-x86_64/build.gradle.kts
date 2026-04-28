/*
 * Per-OS/arch JNI artifact: darwin-x86_64.
 * Ships libschnorr256k1_jni.dylib as a classpath resource at
 * /com/vitorpamplona/schnorr256k1/native/darwin-x86_64/, which NativeLoader
 * (in :schnorr256k1's jvmMain) extracts and System.load()s at runtime.
 */
import org.gradle.internal.os.OperatingSystem

plugins {
    `java-library`
    alias(libs.plugins.vanniktech.maven.publish)
}

group = rootProject.group
version = rootProject.version

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

val jniOs = "darwin"
val jniArch = "x86_64"
val classifier = "$jniOs-$jniArch"
val libFile = "libschnorr256k1_jni.dylib"
val resourceDir = "com/vitorpamplona/schnorr256k1/native/$classifier"

val canBuildHere = OperatingSystem.current().isMacOsX

val nativeOutputDir = layout.buildDirectory.dir("jni-native/$classifier")
val cmakeBuildDir = layout.buildDirectory.dir("cmake-jni/$classifier")

val configureJniLibrary by tasks.registering(Exec::class) {
    group = "build"
    description = "cmake-configures libschnorr256k1_jni for $classifier."
    onlyIf { canBuildHere }
    val outDir = nativeOutputDir.get().asFile
    val buildDir = cmakeBuildDir.get().asFile
    doFirst {
        outDir.mkdirs()
        buildDir.mkdirs()
    }
    workingDir = buildDir
    val javaHome = System.getProperty("java.home") ?: System.getenv("JAVA_HOME") ?: ""
    val jniInclude = if (javaHome.isNotEmpty()) "$javaHome/include" else ""
    commandLine(
        "cmake",
        rootProject.file("jni").absolutePath,
        "-DCMAKE_BUILD_TYPE=Release",
        "-DJNI_INCLUDE_DIR=$jniInclude",
        "-DJNI_INCLUDE_DIR_PLATFORM=$jniInclude/darwin",
        "-DCMAKE_OSX_ARCHITECTURES=x86_64",
        "-DCMAKE_SYSTEM_PROCESSOR=x86_64",
        "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=${outDir.absolutePath}",
    )
}

val buildJniLibrary by tasks.registering(Exec::class) {
    group = "build"
    description = "Builds libschnorr256k1_jni.dylib for $classifier."
    onlyIf { canBuildHere }
    dependsOn(configureJniLibrary)
    workingDir = cmakeBuildDir.get().asFile
    commandLine("cmake", "--build", ".", "--config", "Release")
    inputs.files(rootProject.file("jni/CMakeLists.txt"), rootProject.file("jni/jni_bridge.c"))
    inputs.dir(rootProject.file("libschnorr256k1"))
    outputs.file(nativeOutputDir.map { it.file(libFile) })
}

tasks.named<Copy>("processResources") {
    dependsOn(buildJniLibrary)
    from(nativeOutputDir) {
        include(libFile)
        into(resourceDir)
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()
    coordinates(
        groupId = rootProject.group.toString(),
        artifactId = "schnorr256k1-kmp-jni-jvm-$classifier",
        version = rootProject.version.toString(),
    )
    pom {
        name.set("schnorr256k1-kmp-jni-jvm-$classifier")
        description.set(
            "Prebuilt JNI shared library for schnorr256k1-kmp on $jniOs/$jniArch. " +
                "Loaded automatically by NativeLoader; not intended to be depended on directly.",
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
