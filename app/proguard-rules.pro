# ==============================================================================
# قواعد الحماية والتشفير والتعمية المتقدمة لتطبيق أمان فون (R8 / ProGuard Rules)
# ==============================================================================

# 1. إخفاء أسماء ملفات المصدر ومعلومات التتبع البرمجي لحماية الكود من الهندسة العكسية
-renamesourcefileattribute ""
-keepattributes Exceptions,InnerClasses,Signature

# 2. إزالة كافة سجلات النظام (Logcat Stripping) لمنع تسريب بيانات IMEI وأرقام الهواتف
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

# 3. حماية كيانات قاعدة البيانات (Room Database) ومكتبات التخزين
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# 4. حماية نماذج ونطاقات Firebase و Google Identity
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class androidx.credentials.** { *; }

# 5. حماية نماذج بيانات أمان فون من حذف الحقول أثناء التحسين
-keep class com.example.data.model.** { *; }
-keep class com.example.data.remote.** { *; }
-keep class com.example.util.AmanSecurityEngine { *; }
-keep class com.example.util.DeviceIntegrityChecker { *; }

# 6. حماية مكونات واجهات Jetpack Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

