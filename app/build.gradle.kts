plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    // se aplica aquí el plugin de Protobuf que declaraste en el proyecto raíz
    id("com.google.protobuf")
}

android {
    namespace = "com.example.aerolineaidle"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.aerolineaidle"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Jetpack Compose y demás dependencias existentes …
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    // === DataStore con Proto ===
    // biblioteca de DataStore para objetos tipados (Proto)
    implementation("androidx.datastore:datastore:1.1.7")       // última versión estable:contentReference[oaicite:1]{index=1}
    // versión lite de las clases Java generadas por protoc
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")
}

kotlin {
    jvmToolchain(17)
}

protobuf {
    protoc {
        // versión del compilador protoc usada para generar código
        artifact = "com.google.protobuf:protoc:3.25.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // generar código Java con la opción 'lite'
                java {
                    option("lite")
                }
            }
        }
    }
}

// Opcionalmente especifica dónde están tus archivos .proto
sourceSets {
    named("main") {
        proto {
            srcDir("src/main/proto")
        }
    }
}