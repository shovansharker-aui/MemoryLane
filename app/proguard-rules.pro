# Add project-specific ProGuard rules here.

# Glide — generated API + modules must survive shrinking
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-dontwarn com.bumptech.glide.load.resource.bitmap.VideoDecoder

# Room — entities and DAOs are accessed via generated code/reflection
-keep class com.memorylane.app.data.** { *; }

# ML Kit — model loading uses reflection internally
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# osmdroid — map tile/overlay classes
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# Kotlin coroutines
-dontwarn kotlinx.coroutines.**
