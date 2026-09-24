package com.artsistem.assistme.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** GitHub'daki son sürümün bilgisi. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long
)

/**
 * Uygulamanın kendini güncellemesi.
 *
 * Sürümler GitHub Releases'ta durur (repo herkese açık, kimlik gerekmez).
 * Her sürümde tek APK eki vardır ve adı `AssistMe-<versionCode>.apk` olur;
 * sürüm karşılaştırması bu numarayla yapılır. APK bu makinedeki debug
 * anahtarıyla imzalı olmalı, yoksa Android üzerine kurmayı reddeder.
 */
object AppUpdater {

    private const val TAG = "AppUpdater"
    private const val LATEST_URL = "https://api.github.com/repos/ayazmustafadfn/AssistMe/releases/latest"
    private val ASSET_NAME = Regex("""AssistMe-(\d+)\.apk""")

    fun currentVersionCode(context: Context): Int {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
        else @Suppress("DEPRECATION") info.versionCode
    }

    fun currentVersionName(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

    /** Daha yeni sürüm varsa bilgisini döndürür; yoksa ya da ağ hatasında null. */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "AssistMe-Android")
            }
            val body = try {
                if (conn.responseCode != 200) return@withContext null
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }

            val json = JSONObject(body)
            val assets = json.optJSONArray("assets") ?: return@withContext null
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val code = ASSET_NAME.matchEntire(asset.optString("name"))
                    ?.groupValues?.get(1)?.toIntOrNull() ?: continue
                if (code <= currentVersionCode(context)) return@withContext null
                return@withContext UpdateInfo(
                    versionCode = code,
                    versionName = json.optString("tag_name").removePrefix("v"),
                    notes = json.optString("body").trim(),
                    apkUrl = asset.getString("browser_download_url"),
                    sizeBytes = asset.optLong("size")
                )
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Güncelleme denetlenemedi", e)
            null
        }
    }

    /** APK'yı önbelleğe indirir; [onProgress] 0..1 arası ilerleme verir. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() } // eski indirmeler
            val target = File(dir, "AssistMe-${info.versionCode}.apk")

            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "AssistMe-Android")
            }
            try {
                val total = conn.contentLengthLong.takeIf { it > 0 } ?: info.sizeBytes
                conn.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            target
        }

    /** Android 8+ bu uygulamaya "bilinmeyen uygulama yükleme" izni verilmiş mi. */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** İzin ekranını açar; kullanıcı izni verip geri döner. */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.startActivity(
            Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Sistem kurulum ekranını açar (kullanıcı "Güncelle"ye basar). */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
