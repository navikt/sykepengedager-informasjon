import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.springframework.boot.gradle.tasks.bundling.BootJar

abstract class AppJarArgumentProvider : CommandLineArgumentProvider {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appJar: RegularFileProperty

    override fun asArguments(): Iterable<String> = listOf(appJar.get().asFile.absolutePath)
}

plugins {
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "no.nav.syfo"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

val logstashLogbackEncoderVersion = "9.0"
val kotestVersion = "6.2.3"
val springKotestExtensionVersion = "1.3.0"
val mockkVersion = "1.14.11"
val tokenSupportVersion = "6.0.12"
val kotlinxCoroutinesVersion = "1.11.0"
extra["kotlin-coroutines.version"] = kotlinxCoroutinesVersion
val springmockkVersion = "5.0.1"
val postgresEmbeddedVersion = "2.2.2"
val postgresRuntimeVersion = "18.4.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-jackson2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.22.1")
    implementation("no.nav.security:token-client-spring:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-spring:$tokenSupportVersion")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashLogbackEncoderVersion")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")
    testImplementation("io.kotest:kotest-extensions-spring:$kotestVersion")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("io.zonky.test:embedded-postgres:$postgresEmbeddedVersion")
    testImplementation(platform("io.zonky.test.postgres:embedded-postgres-binaries-bom:$postgresRuntimeVersion"))
    testImplementation("no.nav.security:token-validation-spring-test:$tokenSupportVersion")
    testImplementation("com.ninja-squad:springmockk:$springmockkVersion")
    testCompileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$kotlinxCoroutinesVersion")
}

tasks {
    val bootJarTask = named<BootJar>("bootJar") {
        this.archiveFileName.set("app.jar")
    }

    val verifyCoroutineRuntime =
        register<JavaExec>("verifyCoroutineRuntime") {
            group = "verification"
            description = "Verifies the packaged coroutine runtime and its cancellation ABI."
            dependsOn(bootJarTask, named("testClasses"))
            classpath = files(sourceSets["test"].output, configurations["runtimeClasspath"])
            mainClass.set("no.nav.syfo.CoroutineRuntimeVerifier")
            argumentProviders.add(
                objects.newInstance<AppJarArgumentProvider>().apply {
                    appJar.set(bootJarTask.flatMap { it.archiveFile })
                },
            )
        }

    withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    named<Jar>("jar") {
        enabled = false
    }

    withType<Test> {
        useJUnitPlatform()
    }

    named("check") {
        dependsOn("ktlintCheck", verifyCoroutineRuntime)
    }
}
