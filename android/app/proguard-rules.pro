# Aturan tambahan ProGuard bila minify diaktifkan.
-keep class com.example.falldetection.data.remote.** { *; }
-dontwarn kotlinx.serialization.**
