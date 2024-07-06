import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.springframework.boot.gradle.plugin.SpringBootPlugin;

plugins {
    java
	id("org.springframework.boot") version "2.5.15"
	kotlin("jvm") version "1.8.21"
	kotlin("plugin.spring") version "1.8.21"
	kotlin("kapt") version "1.8.21"
	kotlin("plugin.allopen") version "1.8.21"
}

apply(plugin = "io.spring.dependency-management")

kapt {
	correctErrorTypes = true
}

allOpen {
	annotation("javax.persistence.Entity")
	annotation("javax.persistence.Embeddable")
	annotation("javax.persistence.MappedSuperclass")
}

group = "xyz.deverse"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
	mavenCentral()
	maven { url = uri("https://repo.spring.io/milestone") }
	maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
	implementation(kotlin("stdlib-jdk8"))
	implementation("org.mapstruct:mapstruct:1.5.5.Final")
	kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

	implementation(files("lib/importer-lib-1.2.6.jar"))
	implementation("org.apache.httpcomponents:httpclient:4.5.13")
	implementation("org.apache.commons:commons-text:1.10.0")
	implementation("com.google.guava:guava:32.1.2-jre")
	implementation("org.hibernate:hibernate-validator:5.2.5.Final")
	implementation("javax.validation:validation-api:2.0.1.Final")
	implementation("org.mapstruct:mapstruct-processor:1.3.1.Final")
	implementation("org.mapstruct:mapstruct-jdk8:1.3.1.Final")
	implementation("io.springfox:springfox-swagger-ui:3.0.0")
	implementation("io.springfox:springfox-data-rest:3.0.0")
	implementation("io.springfox:springfox-swagger2:3.0.0")
	implementation("io.springfox:springfox-bean-validators:3.0.0")
	implementation("org.springframework.security:spring-security-oauth2-client")
	implementation("org.apache.poi:poi:5.2.4")
	implementation("org.apache.poi:poi-ooxml:5.2.4")
	implementation("org.apache.commons:commons-lang3:3.11")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-data-rest")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.session:spring-session-core")
	implementation("org.springframework:spring-messaging")
	implementation("org.springframework.security:spring-security-messaging")
	implementation("org.springframework.integration:spring-integration-security")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.retry:spring-retry:1.3.0")
	implementation("org.springframework:spring-aspects")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	runtimeOnly("org.postgresql:postgresql:42.2.27")
}


tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "1.8"
	}
}

tasks.named<Jar>("jar") {
	enabled = false
}

springBoot {
	mainClass.set("xyz.deverse.evendilo.EvendiloCoreApplicationKt")
}