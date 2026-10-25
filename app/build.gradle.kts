plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":agent"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(libs.coroutines.swing)
    runtimeOnly(libs.slf4j.simple)
}

compose.desktop {
    application {
        mainClass = "dev.peekakoog.app.MainKt"
    }
}
