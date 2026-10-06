# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ═══════════════════════════ قواعد Chat Live (R8) ═══════════════════════════

# أرقام أسطر حقيقية في تقارير الانهيار مع إخفاء اسم الملف الأصلي
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# نماذج البيانات: تُبنى وتُقرأ بأسماء حقولها في التخزين المحلي (toJson/fromJson)
# ⇒ لو شوّهها R8 يتفكك التخزين صامتاً بلا أي خطأ بناء.
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }

# أعضاء الـViewModel التي تُقرأ من الواجهة (StateFlow / التحديثات)
-keepclassmembers class com.example.viewmodel.** {
    public <methods>;
}

# مكتبات Compose و AndroidX توفّر قواعدها بنفسها (consumer rules)،
# لكن قواعد الحفاظ على الـComposable تبقى ضرورية للـrelease.
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# انعكاس/أخطاء اختيارية شائعة في مكتبات AndroidX
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlinx.coroutines.debug.**

# الإبقاء على أسماء الأنواع المستخدمة في التتبّع (Stack traces مفهومة)
-keepnames class com.example.**

# أخطاء لينة: لا نفشل البناء بسبب تحذيرات مكتبات خارجية
-ignorewarnings
