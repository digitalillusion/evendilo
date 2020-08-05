import org.jetbrains.kotlin.base.kapt3.KaptOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.kapt3.base.Kapt.kapt

plugins {
	id("org.springframework.boot") version "2.4.0-SNAPSHOT"
	id("io.spring.dependency-management") version "1.0.9.RELEASE"
	kotlin("jvm") version "1.3.72"
	kotlin("plugin.spring") version "1.3.72"
	kotlin("kapt") version "1.3.72"
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
	implementation("org.mapstruct:mapstruct:1.4.0.Beta3")
	kapt("org.mapstruct:mapstruct-processor:1.4.0.Beta3")

	implementation(files("lib/importer-lib-1.2.0.jar"))
	implementation(files("lib/auth-lib-1.0.14.jar"))
	implementation("com.google.guava:guava:29.0-jre")
	implementation("org.hibernate:hibernate-validator:5.2.4.Final")
	implementation("javax.validation:validation-api:2.0.1.Final")
	implementation("org.mapstruct:mapstruct-processor:1.3.1.Final")
	implementation("org.mapstruct:mapstruct-jdk8:1.3.1.Final")
	implementation("io.springfox:springfox-swagger-ui:3.0.0")
	implementation("io.springfox:springfox-data-rest:3.0.0")
	implementation("io.springfox:springfox-swagger2:3.0.0")
	implementation("io.springfox:springfox-bean-validators:3.0.0")
	implementation("org.apache.poi:poi:4.1.2")
	implementation("org.apache.poi:poi-ooxml:4.1.2")
	implementation("org.apache.commons:commons-lang3:3.11")
	implementation("org.springframework.boot:spring-boot-starter-data-rest")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.session:spring-session-core")
	implementation("org.springframework:spring-messaging")
	implementation("org.springframework.security:spring-security-messaging")
	implementation("org.keycloak:keycloak-spring-boot-starter")
	implementation("org.keycloak:keycloak-spring-security-adapter")
	implementation("org.springframework.integration:spring-integration-security")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
	imports {
		mavenBom("org.keycloak.bom:keycloak-adapter-bom:11.0.0")
	}
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
