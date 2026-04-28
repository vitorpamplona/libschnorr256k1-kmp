plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

allprojects {
    group = "com.vitorpamplona"
    version = "1.0.1"
}
