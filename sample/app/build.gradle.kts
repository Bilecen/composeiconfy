plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("io.github.bilecen.iconfy")
}

android {
    namespace = "dev.iconfy.sample"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.iconfy.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.2")
}

// Declare Iconify icons; the plugin downloads them at build time and generates
// offline `Iconfy.Mdi.Home`-style ImageVector accessors into the app.
iconfy {
    packageName.set("dev.iconfy.sample.icons")
    icons {
        add("mdi:home")
        add("tabler:user")
        prefix("lucide") {
            add("heart")
            add("star")
        }
        // Stress the emitter: multi-path filled glyph, hyphenated prefix, and a settings gear.
        add("mdi:github")
        add("material-symbols:settings")
        add("ph:gear-six-fill")
    }
    // Semantic cluster mixing sets → Iconfy.Dashboard.Mdi.Home, Iconfy.Dashboard.Tabler.Settings
    category("Dashboard") {
        prefix("mdi", named = "Nav") { add("home", named = "Main") } // → Iconfy.Dashboard.Nav.Main
        add("tabler:settings")                                        // → Iconfy.Dashboard.Tabler.Settings
    }
}
