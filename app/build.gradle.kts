import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)

  // [ملاحظة للمرحلة القادمة]: تم تعطيل إضافة Google Services مؤقتاً لتمكين البناء وتوليد APK دون الحاجة لملف google-services.json
  // أعد تفعيل السطر التالي عند إضافة ملف google-services.json وربط Firebase الحقيقي:
    // ✅ مُفعَّل (الخطوة 3): يقرأ الإعدادات من app/google-services.json
  // الذي يكتبه الـCI من السرّ GOOGLE_SERVICES_JSON وقت البناء (لا يُرفع للمستودع).
    alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.mujtamauna.ar8vzp"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    // 🎙️ صوت الغرف (Agora): معماريتان 64-بت فقط — مكتبات 32-بت محاذاتها 4KB
    // (تفشل شرط 16KB في بلي)، و arm64-v8a و x86_64 محاذاتهما 16KB
    ndk {
      abiFilters += listOf("arm64-v8a", "x86_64")
    }

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    // [ملاحظة للتوقيع]: تم إلغاء debugConfig المخصص ليستخدم توقيع Debug الافتراضي التلقائي لـ Gradle على GitHub Actions ومحلياً
  }

  buildTypes {
        release {
            isCrunchPngs = false
      // R8: يحذف الكود غير المستخدم ويشوّش الأسماء ⇒ يصعّب الهندسة العكسية
      // ويقلّل زمن بدء التطبيق. القواعد في proguard-rules.pro.
      isMinifyEnabled = true
      // ⚠️ shrinkResources مطفأة عمداً: التطبيق يستخدم موارد بالاسم ديناميكياً
      // (مثل "android.resource://com.aistudio.mujtamauna.ar8vzp/drawable/default_avatar")
      // وتقليص الموارد قد يحذف مورداً مستخدماً فينهار الأفاتار بصمت.
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // ⚠️ أمان: حُذف fallback توقيع الـDebug لنسخة الإصدار.
      // السبب: توقيع Debug مشترك ومعروف ⇒ أي APK معدَّل يُوقَّع بنفس المفتاح
      // ويُثبَّت كتحديث شرعي فوق التطبيق (والأخطر مع قاعدة البيانات: يشير لنفس مشروع Firebase).
      //
      // النسخة الآن:
      //  - مفتاح إصدار موجود (سرّ KEYSTORE_BASE64 بالـCI أو my-upload-key.jks محلياً) ⇒ نسخة إصدار حقيقية.
      //  - غير موجود ⇒ البناء يكمل **بلا توقيع** (APK غير قابل للتثبيت) ⇒ لا خطر انتحال،
      //    والـCI يمنع رفع المخرَج أصلاً في هذه الحالة.
      val releaseKeystore = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
      signingConfig = if (releaseKeystore.exists()) {
        signingConfigs.getByName("release")
      } else {
        null
      }
        }
        debug {
      // 🔑 توقيع ثابت للنسخة التجريبية: بلا هذا، كل تشغيل بناء على GitHub يوقّع بمفتاح Debug
      //    عشوائي جديد ⇒ أندرويد يرفض التثبيت فوق النسخة القديمة ⇒ حذف التطبيق = مسح كل البيانات
      //    (الجلسة + دخول Firebase + ملفك الشخصي) ⇒ يبدو كأن «الدخول لا يُحفظ».
      //    إن وُجد مفتاح الإصدار (سرّ KEYSTORE_BASE64 في CI أو my-upload-key.jks محلياً) ⇒ نوقّع به.
      val dbgKeystore = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
      val dbgPasswordsReady =
        !System.getenv("STORE_PASSWORD").isNullOrBlank() && !System.getenv("KEY_PASSWORD").isNullOrBlank()
      if (dbgKeystore.exists() && dbgPasswordsReady) {
        signingConfig = signingConfigs.getByName("release")
      }
    }

    // ───────────────────────── نسخة التعديلات (Developer Edition) ─────────────────────────
    // نسخة مطوّر مستقلة تُبنى من **نفس الكود** بلا تفريع ولا نسخ مكرر:
    //   • معرّف تطبيق مختلف (.edits) ⇒ تُثبَّت **جنباً إلى جنب** مع نسخة المستخدم ولا تستبدلها
    //   • اسم مختلف: «Chat Live (تعديلات)» — من app/src/edits/res/values/strings.xml
    //   • أيقونة مختلفة اللون — من app/src/edits/res/drawable/ + mipmap-*/
    //   • ترث كل إعدادات debug (توقيع Debug · بلا R8 · قابلة للتنقيح)
    //
    //   نسخة المستخدم  : ./gradlew assembleDebug    ⇒ app-debug.apk
    //   نسخة التعديلات : ./gradlew assembleEdits    ⇒ app-edits.apk
    create("edits") {
      initWith(getByName("debug"))
      applicationIdSuffix = ".edits"
      versionNameSuffix = "-edits"
            isDebuggable = true
    }

    // ───────────────── نسخة اختبار الأداء (Performance Test) ─────────────────
    // الغرض: قياس أثر تحسينات الإصدار (R8 + بلا تنقيح) على جهازك **قبل** التوقيع الحقيقي.
    //   • ترث كل إعدادات release: isMinifyEnabled (R8) + proguard-rules + بلا تنقيح
    //   • توقيع Debug التلقائي ⇒ **قابلة للتثبيت** — للاختبار فقط، **ليست للنشر**
    //   • معرّف مختلف (.perf) ⇒ تُثبَّت جنباً إلى جنب ولا تلمس بيانات النسختين الأخريين
    create("perf") {
      initWith(getByName("release"))
      applicationIdSuffix = ".perf"
      versionNameSuffix = "-perf"
            signingConfig = signingConfigs.getByName("debug")
      // الاسم «Chat Live (اختبار سرعة)» يأتي من app/src/perf/res/values/strings.xml
      // (لا نستخدم resValue: ميزة توليد الموارد من Gradle مطفأة افتراضياً)
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

// [ملاحظة للمرحلة القادمة]: تم تعطيل إعدادات googleServices مؤقتاً لعدم وجود ملف google-services.json.
// ✅ WARN: لو غاب google-services.json لا يفشل البناء (للنسخ المحلية)
googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }
// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.video)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
    // Room غير مستخدم (لا @Entity ولا @Dao بالمشروع) — التخزين عبر data/LocalStore.kt
  // implementation(libs.androidx.room.ktx)
  // implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.coil.video)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.ui)
  // 🎙️ صوت الغرف الحيّة (بلا استخدام بعد — يُفعَّل بمرحلة لاحقة)
  implementation(libs.agora.voice.sdk)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
    // ✅ مُفعَّل (الخطوة 3): تخزين بيانات المستخدمين سحابياً — خطة Spark المجانية
  implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
    implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
    // شبكة غير مستخدمة إطلاقاً (صفر import بالمشروع) — فعّلها عند إضافة API حقيقي
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
    // معطّلان: لا يوجد @Entity/@Dao ولا @JsonClass بأي ملف بالمشروع،
  // فكانا يفحصان كل الكود بكل بناء بلا فائدة ويرميان تحذير KSP/IntelliJ.
  // فعّلهما لو استخدمت Room أو Moshi codegen فعلياً.
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
