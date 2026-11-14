# Nexus Proguard Rules
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class com.nexus.planner.data.** { *; }
