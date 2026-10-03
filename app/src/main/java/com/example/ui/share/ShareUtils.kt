package com.example.ui.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.model.Post
import com.example.model.PostMediaType
import com.example.model.UserProfile
import java.io.File

/**
 * المشاركة الخارجية (قائمة مشاركة أندرويد الأصلية).
 *
 * ملاحظة: مشاركة ملف تتطلب FileProvider مسجّلاً بالمانيفست بالصلاحية
 * "${applicationId}.fileprovider" وملف المسارات res/xml/file_paths.xml.
 */

private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

/**
 * يشارك المنشور: نصه ووسمه، ومعه ملف الصورة أو الفيديو إن وُجد.
 * لو تعذّر تجهيز الملف لأي سبب، يرجع تلقائياً لمشاركة النص فقط بدل ما يفشل.
 */
fun sharePostExternally(context: Context, post: Post) {
        val shareText = buildString {
        if (post.content.isNotBlank()) {
            appendLine(post.content)
        }
        if (!post.tag.isNullOrBlank()) {
            appendLine(post.tag)
        }
        if (isNotEmpty()) appendLine()
        appendLine("منشور من ${post.authorName} على تطبيق Chat Live")
        appendLine()
        appendLine("افتح المنشور داخل التطبيق:")
        append("chatlive://post/${post.id}")
        }

    val mediaFile = if (post.mediaType != PostMediaType.NONE && post.mediaUri.isNotBlank()) {
        File(post.mediaUri).takeIf { it.exists() && it.length() > 0L }
    } else {
        null
    }

    val mediaUri = mediaFile?.let { file ->
        try {
            FileProvider.getUriForFile(
                context,
                context.packageName + FILE_PROVIDER_SUFFIX,
                file
            )
        } catch (e: Exception) {
            null
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        if (mediaUri != null) {
            type = if (post.mediaType == PostMediaType.SHORT_VIDEO) "video/mp4" else "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, mediaUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    launchChooser(context, intent, "مشاركة المنشور")
}

/**
 * يشارك بطاقة تعريف بالملف الشخصي (الاسم واسم المستخدم والنبذة).
 */
fun shareProfileExternally(context: Context, profile: UserProfile) {
    val shareText = buildString {
        appendLine("تابعني على تطبيق Chat Live 👋")
        if (profile.name.isNotBlank()) {
            appendLine(profile.name)
        }
        if (profile.handle.isNotBlank()) {
            appendLine(profile.handle)
        }
        if (profile.bio.isNotBlank()) {
            appendLine()
            appendLine(profile.bio)
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }

    launchChooser(context, intent, "مشاركة الملف الشخصي")
}

private fun launchChooser(context: Context, intent: Intent, title: String) {
    try {
        val chooser = Intent.createChooser(intent, title)
        // لازم عند الإطلاق من سياق ليس Activity
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        // لا يوجد تطبيق يستقبل المشاركة، أو منعها النظام — لا نسقط التطبيق
    }
}
