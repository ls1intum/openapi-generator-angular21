plugins {
    java
    `java-library`
    `maven-publish`
    signing
}

group = "de.tum.cit.aet"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

val openapiGeneratorCli by configurations.creating

dependencies {
    val openapiGeneratorVersion = "7.18.0"

    // OpenAPI Generator core dependency
    implementation("org.openapitools:openapi-generator:$openapiGeneratorVersion")
    openapiGeneratorCli("org.openapitools:openapi-generator-cli:$openapiGeneratorVersion")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.register<JavaExec>("generateExample") {
    dependsOn(tasks.named("jar"))
    doFirst {
        delete("build/generated/example")
    }
    classpath = openapiGeneratorCli + files(tasks.named<Jar>("jar").get().archiveFile.get().asFile)
    mainClass.set("org.openapitools.codegen.OpenAPIGenerator")
    args(
        "generate",
        "-g", "angular21",
        "-i", "example/example-openapi.yaml",
        "-o", "build/generated/example"
    )
}

tasks.test {
    useJUnitPlatform()
}

// Register the generator with OpenAPI Generator's SPI
tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name.set("OpenAPI Generator Angular 21")
                description.set("Custom OpenAPI Generator for modern Angular 21 with httpResource and signals")
                url.set("https://github.com/ls1intum/openapi-generator-angular21")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("ls1intum")
                        name.set("LS1 TUM")
                        email.set("krusche@tum.de")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/ls1intum/openapi-generator-angular21.git")
                    developerConnection.set("scm:git:ssh://github.com/ls1intum/openapi-generator-angular21.git")
                    url.set("https://github.com/ls1intum/openapi-generator-angular21")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ls1intum/openapi-generator-angular21")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
