plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":engine"))
    api(libs.koog.agents)
    implementation(libs.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.koog.agents.test)
    testImplementation(libs.coroutines.core)
}

tasks.test {
    useJUnitPlatform()
}
