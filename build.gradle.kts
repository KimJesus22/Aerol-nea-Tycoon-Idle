plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
    // plugin de protoc para generar las clases Java a partir de los .proto
    id("com.google.protobuf") version "0.9.5" apply false
}