plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    // Publishes to the Gradle Plugin Portal via `./gradlew publishPlugins`.
    id("com.gradle.plugin-publish") version "1.3.0"
}

group = "io.github.bilecen"
version = "0.1.1"

repositories {
    google()
    mavenCentral()
}

dependencies {
    // AGP variant API (AndroidComponentsExtension) for wiring generated sources.
    // compileOnly: we don't force AGP onto non-Android consumers; wiring is guarded by plugins.withId.
    compileOnly("com.android.tools.build:gradle-api:8.7.3")

    // Minimal JSON parsing for the Iconify API response.
    implementation("com.google.code.gson:gson:2.11.0")

    // SVG → VectorDrawable normalization (shapes, transforms, currentColor). Build-time only.
    implementation("com.android.tools:sdk-common:31.7.3")

    // Our own ImageVector source emitter.
    implementation("com.squareup:kotlinpoet:1.18.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation(gradleTestKit())
}

gradlePlugin {
    // Set these to your real repo before publishing to the Gradle Plugin Portal.
    website.set("https://github.com/Bilecen/composeiconfy")
    vcsUrl.set("https://github.com/Bilecen/composeiconfy")
    plugins {
        create("iconfy") {
            id = "io.github.bilecen.iconfy"
            implementationClass = "dev.iconfy.IconfyPlugin"
            displayName = "Iconfy — Iconify icons for Compose"
            description = "Build-time Iconify → Jetpack Compose ImageVector code generation"
            tags.set(listOf("android", "compose", "icons", "iconify", "codegen"))
        }
    }
}

// java-gradle-plugin auto-creates the "pluginMaven" publication and the plugin marker; enrich POMs.
publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Iconfy")
            description.set("Build-time Iconify → Jetpack Compose ImageVector code generation.")
            url.set("https://github.com/Bilecen/composeiconfy")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("bilecen")
                    name.set("Taha Bilecen")
                }
            }
            scm {
                url.set("https://github.com/Bilecen/composeiconfy")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
