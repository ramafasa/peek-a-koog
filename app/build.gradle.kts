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

dependencies {
    testImplementation(kotlin("test"))
    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.uiTest)
    testImplementation(libs.koog.agents.test)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("snapshots.dir", layout.buildDirectory.dir("snapshots").get().asFile.absolutePath)
}
