# تطبيق مجتمعنا (Mujtamauna) 🌟

تطبيق تواصل اجتماعي عربي متكامل للأندرويد، يجمع بين غرف المحادثات الصوتية والتفاعلية، الألعاب الجماعية الكلاسيكية (طاولة الدومينو الفاخرة)، نظام الفرق والتحديات، والمحفظة الرقمية.

---

## 📱 نوع المشروع وتفاصيل البيئة (Project Type)
- **النوع:** **تطبيق أندرويد أصلي (Native Android)**
- **لغة البرمجة:** Kotlin 2.0+
- **واجهة المستخدم:** Jetpack Compose (Material Design 3)
- **نظام البناء:** Gradle 8+ (Kotlin DSL - `build.gradle.kts`)
- **إصدار جافا المستهدف:** Java / JDK 21
- **مستوى حزمة أندرويد:** `minSdk: 24` (Android 7.0+) | `targetSdk: 36` | `compileSdk: 36`

---

## 🚀 خطوات الرفع على GitHub وبناء APK تلقائياً

المشروع مهيأ بالكامل بملف سير العمل **GitHub Actions** (`.github/workflows/build.yml`). فور رفع الكود إلى فرع `main`، سيقوم GitHub ببناء التطبيق وتوفير ملف `APK` جاهز للتحميل والتثبيت.

### 1. خطوات الرفع لأول مرة:
افتح الطرفية (Terminal) في مجلد المشروع ونفّذ الأوامر التالية:

```bash
# 1. تهيئة مستودع Git محلي
git init

# 2. إضافة جميع الملفات (ملف .gitignore مستثني للملفات غير المرغوبة تلقائياً)
git add .

# 3. حفظ التغييرات
git commit -m "Initial commit - Mujtamauna Native Android App"

# 4. تسمية الفرع الأساسي main
git branch -M main

# 5. ربط المستودع برابط مستودعك على GitHub
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git

# 6. رفع الكود
git push -u origin main
```

### 2. تحميل ملف APK من GitHub:
1. اذهب إلى صفحة مستودعك على GitHub.
2. اضغط على تبويب **Actions** في الأعلى.
3. ستجد مهمة البناء **Build & Package Android APK** تعمل تلقائياً.
4. بعد اكتمال البناء وظهور علامة الصح الخضراء (✔)، ادخل إلى تفاصيل التشغيل.
5. في قسم **Artifacts** بالأسفل، ستجد ملف **`app-debug-apk`** جاهزاً للتحميل بنقرة واحدة بصيغة ZIP يحتوي على ملف الـ APK القابل للتثبيت مباشرة.

---

## 💻 البناء والتشغيل المحلي (Local Build)

### 1. إعداد المفاتيح المحلية (اختياري)
انسخ ملف الإعداد النموذجي:
```bash
cp .env.example .env
```
*(ملاحظة: ملف `.env` مستبعد تماماً من الرفع لحماية خصوصيتك وأمان حساباتك).*

### 2. بناء ملف APK للاختبار
باستخدام أداة Gradle Wrapper المضمنة:
```bash
# لأنظمة Linux / macOS:
./gradlew assembleDebug

# لأنظمة Windows:
gradlew.bat assembleDebug
```
يتم توليد ملف الـ APK في المسار:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. التثبيت المباشر على الهاتف عبر ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. تشغيل الاختبارات
```bash
./gradlew testDebugUnitTest
```

---

## 📂 هيكل المشروع (Project Structure)
```
├── .github/
│   └── workflows/
│       └── build.yml               # ملف الأتمتة لبناء الـ APK على GitHub Actions
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml # إعدادات التطبيق والصلاحيات
│   │   │   ├── java/com/example/
│   │   │   │   ├── model/          # نماذج البيانات (Auth, Domino Game, Rooms, Teams)
│   │   │   │   ├── ui/             # واجهات وتصميم Jetpack Compose
│   │   │   │   │   ├── components/ # العناصر المشتركة (TopBar, BottomNav, etc.)
│   │   │   │   │   ├── screens/    # الشاشات (Auth, DominoTable, Chat, Feed, etc.)
│   │   │   │   │   └── theme/      # الهوية البصرية والألوان الملكية
│   │   │   │   └── viewmodel/      # نموذج العرض الموحد SocialAppViewModel
│   │   │   └── res/                # الموارد والقيم والنصوص والأيقونات
│   │   └── test/                   # اختبارات Robolectric و Unit Tests
│   └── build.gradle.kts            # إعدادات تطبيق أندرويد والمكتبات
├── gradle/
│   ├── libs.versions.toml          # جدول إصدارات المكتبات
│   └── wrapper/                    # ملفات Gradle Wrapper
├── .env.example                    # نموذج المتغيرات والمفاتيح
├── .gitignore                      # استبعاد مخلفات البناء والأسرار والمفاتيح
├── gradlew / gradlew.bat           # مشغل Gradle المستقل
├── README.md                       # هذا الدليل التوثيقي
└── settings.gradle.kts             # إعدادات المشروع وجرادل
```

---

## 🔒 الأمان وحماية البيانات
- **عزل المفاتيح:** ملفات `.env` و`local.properties` و`secrets.properties` مستبعدة نهائياً من الرفع عبر `.gitignore`.
- **مفاتيح التوقيع:** مفاتيح `*.keystore` و`*.jks` مستبعدة بالكامل ولا يتم رفعها إلى المستودع العام.
