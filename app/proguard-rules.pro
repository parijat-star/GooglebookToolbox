# Keep kotlinx.serialization @Serializable models and generated serializers for WorkspaceSnapshot persistence.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.droid.googlebooktoolbox.apprestorer.model.**$$serializer { *; }
-keepclassmembers class com.droid.googlebooktoolbox.apprestorer.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.droid.googlebooktoolbox.apprestorer.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.droid.googlebooktoolbox.apprestorer.model.** { *; }

# Keep Feature Checker models & snapshots
-keep class com.droid.googlebooktoolbox.featurechecker.data.CompanionPhoneSnapshot { *; }
-keep class com.droid.googlebooktoolbox.featurechecker.data.DeviceSystemSnapshot { *; }
-keep class com.droid.googlebooktoolbox.featurechecker.model.** { *; }
-dontwarn java.lang.invoke.StringConcatFactory
