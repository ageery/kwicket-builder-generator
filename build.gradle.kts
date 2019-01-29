import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

val kotlinVersion = "1.3.11"
val wicketVersion = "8.2.0"
val kotlinxHtmlVersion = "0.6.11"
val kotlinPoetVersion = "1.0.1"

plugins {
    kotlin("jvm") version "1.3.11"
    id("org.jetbrains.dokka") version "0.9.16"
    `maven-publish`
    id("java-library") // see: https://youtrack.jetbrains.com/issue/KT-28355 -- fixed in 1.3.20
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
    implementation(group = "org.apache.wicket", name = "wicket-extensions", version = wicketVersion)
    implementation(group = "org.apache.wicket", name = "wicket-devutils", version = wicketVersion)
    implementation(group = "org.jetbrains.kotlinx", name = "kotlinx-html-jvm", version = kotlinxHtmlVersion)
    implementation(group = "com.squareup", name = "kotlinpoet", version = kotlinPoetVersion)
    api(group = "org.jetbrains.kotlin", name = "kotlin-reflect", version = kotlinVersion)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val dokkaJavadocTask = tasks.withType<DokkaTask> {
    outputFormat = "html"
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://ci.apache.org/projects/wicket/apidocs/8.x/")
    })
    externalDocumentationLink(delegateClosureOf<DokkaConfiguration.ExternalDocumentationLink.Builder> {
        url = URL("https://square.github.io/kotlinpoet/1.x/kotlinpoet/")
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components.getByName("java"))
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}
