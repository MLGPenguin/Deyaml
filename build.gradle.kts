plugins {
    kotlin("jvm") version "2.1.20" apply false
}

subprojects {
    group = "com.github.mlgpenguin"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}