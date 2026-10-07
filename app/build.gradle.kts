import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val releaseSigningPropertyNames = listOf(
    "MHP3_RELEASE_STORE_FILE",
    "MHP3_RELEASE_STORE_PASSWORD",
    "MHP3_RELEASE_KEY_ALIAS",
    "MHP3_RELEASE_KEY_PASSWORD",
)

fun releaseSigningValue(name: String): String? =
    providers.gradleProperty(name).orNull?.takeIf(String::isNotBlank)
        ?: providers.environmentVariable(name).orNull?.takeIf(String::isNotBlank)

val missingReleaseSigningProperties = releaseSigningPropertyNames.filter { releaseSigningValue(it) == null }
val releaseSigningConfigured = missingReleaseSigningProperties.isEmpty()

val verifyReleaseSigningConfiguration = tasks.register("verifyReleaseSigningConfiguration") {
    group = "verification"
    description = "Fails closed unless an external release keystore and all signing properties are configured."
    doLast {
        val missing = releaseSigningPropertyNames.filter { releaseSigningValue(it) == null }
        check(missing.isEmpty()) {
            "Release signing configuration missing. Set ${missing.joinToString(", ")} " +
                "through Gradle properties or environment variables. Release will not use debug signing."
        }
        val storePath = requireNotNull(releaseSigningValue("MHP3_RELEASE_STORE_FILE"))
        check(file(storePath).isFile) {
            "Release signing keystore was not found at the configured MHP3_RELEASE_STORE_FILE path."
        }
    }
}

tasks.configureEach {
    if (name == "assembleRelease" || name == "packageRelease" || name == "bundleRelease") {
        dependsOn(verifyReleaseSigningConfiguration)
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.waillio.mhp3rdcompanion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.waillio.mhp3rdcompanion"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = "MHP3rd Companion"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }

    testOptions { unitTests.isIncludeAndroidResources = true }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("externalRelease") {
                storeFile = file(requireNotNull(releaseSigningValue("MHP3_RELEASE_STORE_FILE")))
                storePassword = requireNotNull(releaseSigningValue("MHP3_RELEASE_STORE_PASSWORD"))
                keyAlias = requireNotNull(releaseSigningValue("MHP3_RELEASE_KEY_ALIAS"))
                keyPassword = requireNotNull(releaseSigningValue("MHP3_RELEASE_KEY_PASSWORD"))
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = if (releaseSigningConfigured) {
                signingConfigs.getByName("externalRelease")
            } else {
                null
            }
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.5")
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("org.robolectric:robolectric:4.16.1")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
