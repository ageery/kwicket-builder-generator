import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

val wicketVersion = "8.2.0"
val kotlinxHtmlVersion = "0.6.11"
val kotlinPoetVersion = "1.0.0"
val mavenPubName = "mavenJavaLibrary"

plugins {
    kotlin("jvm") version "1.3.11"
    id("org.jetbrains.dokka") version "0.9.16"
    id("maven-publish")
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

val dokkaJavadocTask = tasks.withType<DokkaTask> {
    outputFormat = "html"
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://ci.apache.org/projects/wicket/apidocs/8.x/")
    })
    cacheRoot = "default"
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
    setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
}

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(dokkaJavadocTask)
    classifier = "javadoc"
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>(mavenPubName) {
            from(components.getByName("java"))
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}