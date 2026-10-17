# ==============================================================================
# قواعد الحماية والتشفير والتعمية المتقدمة لتطبيق أمان فون (R8 / ProGuard Rules)
# ==============================================================================

# 1. إخفاء أسماء ملفات المصدر ومعلومات التتبع البرمجي لحماية الكود من الهندسة العكسية
-renamesourcefileattribute ""
-keepattributes Exceptions,InnerClasses,Signature,EnclosingMethod

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

# 4. حماية نماذج ونطاقات Firebase و Google Identity (بقية القواعد تأتي تلقائياً من AAR consumer rules)
-keepattributes *Annotation*
-keep class com.google.firebase.firestore.** { *; }

# 5. حماية نماذج بيانات أمان فون من حذف الحقول أثناء التحسين (Firestore toObjects يعتمد على الانعكاس)
-keep class com.example.data.model.** { *; }
-keep class com.example.data.remote.** { *; }
-keep class com.example.util.AmanSecurityEngine { *; }
-keep class com.example.util.DeviceIntegrityChecker { *; }
-keep class com.example.service.** { *; }

# 6. Kotlin metadata (reflection)
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { public <methods>; }

# 7. تحذيرات مكتبات اختيارية غير موجودة على أندرويد (OkHttp / gRPC / Recaptcha)
-dontwarn androidx.compose.**
-dontwarn com.google.android.gms.internal.recaptchabase.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn okhttp3.internal.platform.**
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn kotlinx.serialization.**
-dontwarn io.grpc.**
-dontwarn com.google.firebase.appcheck.debug.**
