# ── PDFToolkit Application Rules ───────────────────────────
-keep class com.yashodatech.pdftoolkit.** { *; }
-keepclassmembers class com.yashodatech.pdftoolkit.** { *; }
-keepclassmembernames class com.yashodatech.pdftoolkit.** { *; }

# ── App Startup & InitializationProvider Fix ──────────────────
-keep class * implements androidx.startup.Initializer { *; }
-keepclassmembers class * implements androidx.startup.Initializer {
    public <init>();
}
-keep class androidx.startup.** { *; }
-dontwarn androidx.startup.**

# ── ProfileInstaller & Baseline Profile ──────────────────────
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**

# ── Kotlin Coroutines & ServiceLoader ────────────────────────
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Firebase & Google Play Services ──────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── ML Kit Document Scanner ──────────────────────────────────
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ── iText 7 PDF Core & BouncyCastle ──────────────────────────
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
-keep class org.slf4j.** { *; }
-dontwarn org.slf4j.**

# ── Image Cropper & Coil ─────────────────────────────────────
-keep class com.canhub.cropper.** { *; }
-dontwarn com.canhub.cropper.**
-keep class io.coil_kt.** { *; }
-dontwarn io.coil_kt.**

# ── Jetpack DataStore & Preferences ──────────────────────────
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Jetpack Compose & Material 3 ──────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── General Reflection & Attributes Preserve ────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, SourceFile, LineNumberTable
-keep class kotlin.** { *; }
-dontwarn kotlin.**