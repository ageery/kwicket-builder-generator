import com.jfrog.bintray.gradle.BintrayExtension
import groovy.lang.GroovyObject
import org.gradle.jvm.tasks.Jar
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import java.net.URL

val publicationName = "maven"

val wicketVersion: String by project
val kotlinxHtmlVersion: String by project
val kotlinPoetVersion: String by project

val bintrayUser: String? by project
val bintrayKey: String? by project

plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.jetbrains.dokka")
    id("net.researchgate.release")
    id("com.jfrog.bintray")
    id("com.jfrog.artifactory")
}

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
    api(group = "org.jetbrains.kotlin", name = "kotlin-reflect")
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
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val javadocJar by tasks.creating(Jar::class) {
    dependsOn(dokkaJavadocTask)
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>(publicationName) {
            from(components.getByName("java"))
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}

bintray {
    user = bintrayUser
    key = bintrayKey
    setPublications(publicationName)
    publish = true
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "kwicket"
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/ageery/kwicket.git"
        githubRepo = "ageery/kwicket"
        githubReleaseNotesFile = "README.md"
        version(delegateClosureOf<BintrayExtension.VersionConfig> {
            name = "$version"
            vcsTag = "kwicket-$version"
        })
    })
}

artifactory {
    setContextUrl("https://oss.jfrog.org/artifactory")
    publish(delegateClosureOf<PublisherConfig> {
        repository(delegateClosureOf<GroovyObject> {
            setProperty("repoKey", "oss-snapshot-local")
            setProperty("username", bintrayUser)
            setProperty("password", bintrayKey)
            setProperty("maven", true)
        })
        defaults(delegateClosureOf<GroovyObject> {
            invokeMethod("publications", publicationName)
            setProperty("publishArtifacts", true)
            setProperty("publishPom", true)
        })
    })
}
