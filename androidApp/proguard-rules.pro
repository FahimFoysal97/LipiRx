# Suppress warnings for JVM-only classes referenced by Ktor / kotlinx / libraries
-dontwarn java.lang.management.**
-dontwarn javax.annotation.**
-dontwarn org.slf4j.**

# Jetpack App Startup & Initializers
-keep class androidx.startup.** { *; }
-keep class * implements androidx.startup.Initializer {
    public <init>();
}

# Jetpack WorkManager & Room WorkDatabase
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.impl.WorkDatabase { *; }
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}

# Keep line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,ElementValuePairs,Signature,InnerClasses,EnclosingMethod

# Kotlinx Serialization & Domain DTOs
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}
-keep class com.snehonir.lipirx.data.remote.dto.** { *; }
-keep class com.snehonir.lipirx.domain.agent.koog.dto.** { *; }
-keep class com.snehonir.lipirx.domain.agent.** { *; }
-keep class com.snehonir.lipirx.domain.mcp.** { *; }
-keep class com.snehonir.lipirx.domain.model.** { *; }

# LiteRT & LiteRT-LM (TFLite / Edge AI)
-keep class com.google.ai.edge.** { *; }
-keep class org.tensorflow.lite.** { *; }

# Koin Dependency Injection
-keep class io.insertkoin.** { *; }

# SQLDelight Database
-keep class app.cash.sqldelight.** { *; }
-keep class com.snehonir.lipirx.data.local.** { *; }

# Ktor & Napier Logging
-keep class io.ktor.** { *; }
-keep class io.github.aakira.napier.** { *; }


