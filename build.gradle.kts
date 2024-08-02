plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "me.constructor"
version = "1.3"

repositories {
    mavenCentral()

    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("org.json:json:20231013")
    implementation("com.github.Querz:NBT:nbt7-SNAPSHOT")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("me.tongfei:progressbar:0.10.0")
//    implementation("dev.reimer:progressbar-ktx:0.1.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}
tasks.jar {
    manifest {
        attributes["Main-Class"] = "me.constructor.MainKt"
    }
}