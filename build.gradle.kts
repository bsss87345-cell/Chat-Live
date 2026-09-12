// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
  alias(libs.plugins.secrets) apply false

  // [ملاحظة للمرحلة القادمة]: تم تعطيل إضافة Google Services مؤقتاً لعدم وجود ملف google-services.json أو ربط حقيقي بـ Firebase.
  // أعد تفعيل هذا السطر عند ربط مشروع Firebase وإضافة google-services.json:
  // alias(libs.plugins.google.services) apply false
}
