plugins {
    kotlin("jvm") version "2.3.0"
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("reflect"))
    implementation("org.yaml:snakeyaml:2.4")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

publishing.publications.create<MavenPublication>("maven") {
    groupId = "com.github.mlgpenguin"
    artifactId = "DeYaml"
//    version = # gradle.properties
    from(components["java"])
}

