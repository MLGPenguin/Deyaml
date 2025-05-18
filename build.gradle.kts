plugins {
    kotlin("jvm") version "2.1.20"
}

group = "com.github.mlgpenguin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.yaml:snakeyaml:2.4")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}