package com.seerah.data.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val latestVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val isUpdateAvailable: Boolean
)

sealed class UpdateDownloadProgress {
    data object Idle : UpdateDownloadProgress()
    data class Downloading(val percent: Int, val downloadedBytes: Long, val totalBytes: Long) : UpdateDownloadProgress()
    data class ReadyToInstall(val apkFile: File) : UpdateDownloadProgress()
    data class Error(val message: String) : UpdateDownloadProgress()
}

/**
 * نظام التحديث المباشر من داخل تطبيق سِيرَة عبر GitHub Releases.
 * لا يبدأ أي تنزيل تلقائيًا إلا بعد موافقة المستخدم وضغطه على زر التحقق/التحديث.
 */
class SeerahAppUpdater(
    private val context: Context,
    private val githubOwner: String = DEFAULT_GITHUB_OWNER,
    private val githubRepo: String = DEFAULT_GITHUB_REPO
) {

    fun currentVersionName(): String {
        return runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0.1"
        }.getOrDefault("1.0.1")
    }

    suspend fun checkLatestRelease(): Result<AppReleaseInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val apiUrl = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
            val conn = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 12_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Seerah-Android-App")
            }

            val code = conn.responseCode
            if (code == 404) {
                error("لم يتم العثور على إصدار منشور بعد في المستودع ($githubOwner/$githubRepo).")
            }
            if (code !in 200..299) {
                error("تعذر الاتصال بخدمة التحديثات (رمز الاستجابة: $code).")
            }

            val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            conn.disconnect()

            val root = JSONObject(body)
            val tagName = root.optString("tag_name", "").trim()
            val cleanRemote = tagName.removePrefix("v").removePrefix("V")
            val cleanLocal = currentVersionName().removePrefix("v").removePrefix("V")

            val assets = root.optJSONArray("assets")
            var apkUrl = ""
            var apkSize = 0L
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isBlank()) {
                error("الإصدار ($tagName) منشور ولكن لا يحتوي على ملف APK مرفق.")
            }

            AppReleaseInfo(
                latestVersion = tagName.ifBlank { cleanRemote },
                releaseTitle = root.optString("name", "إصدار $tagName"),
                releaseNotes = root.optString("body", "تحديث جديد لتطبيق سِيرَة."),
                apkDownloadUrl = apkUrl,
                apkSizeBytes = apkSize,
                isUpdateAvailable = isNewerVersion(cleanRemote, cleanLocal)
            )
        }
    }

    fun downloadUpdateApk(apkUrl: String): Flow<UpdateDownloadProgress> = flow {
        emit(UpdateDownloadProgress.Downloading(0, 0L, 0L))
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val targetFile = File(updatesDir, "seerah-latest-update.apk")
            if (targetFile.exists()) targetFile.delete()

            val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("User-Agent", "Seerah-Android-App")
            }

            val totalBytes = conn.contentLengthLong.coerceAtLeast(0L)
            var downloaded = 0L
            var lastPercent = -1

            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(16_384)
                    var read = input.read(buffer)
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        val pct = if (totalBytes > 0L) ((downloaded * 100L) / totalBytes).toInt().coerceIn(0, 100) else 0
                        if (pct != lastPercent) {
                            lastPercent = pct
                            emit(UpdateDownloadProgress.Downloading(pct, downloaded, totalBytes))
                        }
                        read = input.read(buffer)
                    }
                }
            }
            conn.disconnect()
            emit(UpdateDownloadProgress.ReadyToInstall(targetFile))
        } catch (e: Exception) {
            emit(UpdateDownloadProgress.Error("فشل تحميل التحديث: ${e.message ?: "خطأ في الاتصال"}"))
        }
    }.flowOn(Dispatchers.IO)

    fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun installApk(apkFile: File): Result<Unit> = runCatching {
        if (!canInstallPackages()) {
            openInstallPermissionSettings()
            error("يرجى تفعيل إذن «تثبيت التطبيقات غير المعروفة» لتطبيق سِيرَة ثم الضغط على تثبيت التحديث مجددًا.")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val rParts = remote.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val lParts = local.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val l = lParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    companion object {
        const val DEFAULT_GITHUB_OWNER = "nabelmuh525-design"
        const val DEFAULT_GITHUB_REPO = "seerah-app"
    }
}
