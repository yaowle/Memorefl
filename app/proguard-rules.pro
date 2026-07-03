# ── MemoRefl ProGuard Rules ──
# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Room ──
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.paging.**

# ── Kotlin Serialization ──
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep our serializable data models
-keep,includedescriptorclasses class com.example.myapplication.KnowledgeNode { *; }
-keep,includedescriptorclasses class com.example.myapplication.NoteBlock { *; }
-keep,includedescriptorclasses class com.example.myapplication.NoteBlock$** { *; }
-keep,includedescriptorclasses class com.example.myapplication.NoteContent { *; }
-keep,includedescriptorclasses class com.example.myapplication.CalendarEvent { *; }
-keep,includedescriptorclasses class com.example.myapplication.NodeType { *; }

# Keep Compose safe
-keep class ** { @kotlinx.serialization.Serializable <fields>; }

# ── Coil ──
-dontwarn coil.**
-keep class coil.** { *; }

# ── General ──
-keepattributes Signature
-keepattributes Exceptions
