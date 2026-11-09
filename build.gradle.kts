plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

// JitPack (jitpack.io, jitpack.yml) sets JITPACK=true and VERSION (the tag or commit it builds), and
// serves what lands in ~/.m2 under com.github.vitorpamplona.libschnorr256k1-kmp: the group every
// module's POM and Gradle module metadata name the others by. It has no signing keys.
val jitpack = providers.environmentVariable("JITPACK").orNull == "true"

allprojects {
    group = if (jitpack) "com.github.vitorpamplona.libschnorr256k1-kmp" else "com.vitorpamplona.schnorr256k1"
    version = providers.environmentVariable("VERSION").orNull?.takeIf { jitpack } ?: "1.1.0"
    extra["signPublications"] = !jitpack
}
