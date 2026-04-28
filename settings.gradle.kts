pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "schnorr256k1-kmp"
include(":schnorr256k1")

// Per-OS/arch JNI artifacts that bundle the prebuilt libschnorr256k1_jni shared
// library as a classpath resource. NativeLoader extracts the matching one at
// runtime. Layout mirrors ACINQ's secp256k1-kmp-jni-jvm-{linux,darwin}.
listOf(
    "linux-x86_64",
    "linux-aarch64",
    "darwin-x86_64",
    "darwin-aarch64",
).forEach { classifier ->
    val name = ":jni-jvm-$classifier"
    include(name)
    project(name).projectDir = file("jni/jvm/$classifier")
}
