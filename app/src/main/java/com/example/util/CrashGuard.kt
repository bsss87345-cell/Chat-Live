package com.example.util

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * 🩺 أداة تشخيص **مؤقتة** — تُعرض عند الحاجة فقط ثم تُحذف.
 *
 * المشكلة التي تحلها: التطبيق يختفي عند الإقلاع بلا أي رسالة مفهومة على الجوال،
 * ولا يوجد جهاز كمبيوتر لقراءة سجل النظام (logcat).
 *
 * الطريقة:
 *  1) [install] يسجّل معالجاً لأي استثناء غير مُعالَج ⇒ يكتب نصّ الخطأ كاملاً في ملف
 *     داخل مساحة التطبيق (`filesDir/last_crash.txt`) ثم يترك النظام ينهي التطبيق كالمعتاد.
 *  2) [showLastCrashScreen] — عند التشغيل التالي — **يعرض نص الخطأ بشاشة كاملة**
 *     (بلا Compose وبلا Firebase) بدل تشغيل التطبيق ⇒ تبقى الشاشة ظاهرة حتى تُصوَّر أو تُقرأ.
 *     زر واحد: «حذف التقرير ومتابعة التطبيق» ⇒ يحذف الملف ويعيد تشغيل النشاط.
 *
 * ⚠️ لا تعتمد على هذه الأداة في الإصدار النهائي: تُحذف بعد انتهاء التشخيص.
 */
object CrashGuard {

    private const val TAG = "CrashGuard"
    private const val FILE_NAME = "last_crash.txt"
    private const val MAX_CHARS = 12_000

    private var installed = false

    /** يثبّت المعالج مرة واحدة (يُستدعى من `MainActivity.onCreate`). */
    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                File(appContext.filesDir, FILE_NAME).writeText(writer.toString().take(MAX_CHARS))
            } catch (e: Throwable) {
                Log.e(TAG, "تعذّر حفظ تقرير الانهيار", e)
            }
            // نترك النظام ينهي التطبيق كالمعتاد (لا نُخفى المشكلة)
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** نصّ آخر انهيار، أو null لو لا يوجد. */
    fun read(context: Context): String? = try {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.readText() else null
    } catch (e: Throwable) {
        null
    }

    /** يحذف التقرير (بعد عرضه أو عند «متابعة التطبيق»). */
    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (e: Throwable) {
            // تجاهل — سيُستبدل عند أول انهيار جديد
        }
    }
/**
     * إن وُجد تقرير انهيار: يعرضه بشاشة كاملة ويوقف المتابعة.
     * يرجع true لو عُرضت الشاشة (على المستدعي أن يوقف `onCreate`).
     */
    fun showLastCrashScreen(activity: Activity): Boolean {
        val trace = read(activity) ?: return false

        val textView = TextView(activity).apply {
            text = "سبب توقّف التطبيق في التشغيل السابق:\n\nتقرير التشخيص (انسخه أو صوّره):\n\n$trace"
            textSize = 10f
            setPadding(28, 40, 28, 28)
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(activity).apply { addView(textView) }
        val button = Button(activity).apply {
            text = "حذف التقرير ومتابعة التطبيق"
            setOnClickListener {
                clear(activity)
                activity.recreate()
            }
        }
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                scroll,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            addView(
                button,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        return try {
            activity.setContentView(root)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "تعذّر عرض شاشة التشخيص", e)
            false
        }
    }
}
