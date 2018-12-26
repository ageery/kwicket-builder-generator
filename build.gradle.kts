import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val wicketVersion = "8.2.0"
val kotlinxHtmlVersion = "0.6.11"
val kotlinPoetVersion = "1.0.0"

plugins {
    kotlin("jvm") version "1.3.11"
}

group = "org.kwicket"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    implementation(group = "org.apache.wicket", name = "wicket-core", version = wicketVersion)
    implementation(group = "org.apache.wicket", name = "wicket-devutils", version = wicketVersion)
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-html-jvm", version = kotlinxHtmlVersion)
    implementation(group = "com.squareup", name = "kotlinpoet", version = kotlinPoetVersion)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}