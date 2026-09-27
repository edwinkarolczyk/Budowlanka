package pl.edwin.budowlanka.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val apkUrl: String
)

object UpdateChecker {
    const val manifestUrl =
        "https://raw.githubusercontent.com/edwinkarolczyk/Budowlanka/development-0.5/updates/manifest.json"

    suspend fun check(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val c = (URL(manifestUrl + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
            }
            val body = c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            val j = JSONObject(body)
            val remote = j.getInt("versionCode")
            val current = context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            if (remote > current) {
                UpdateInfo(
                    versionCode = remote,
                    versionName = j.getString("versionName"),
                    changelog = j.optString("changelog"),
                    apkUrl = j.getString("apkUrl")
                )
            } else null
        }.getOrNull()
    }

    fun download(context: Context, info: UpdateInfo) {
        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("Budowlanka ${info.versionName}")
            .setDescription("Pobieranie aktualizacji")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Budowlanka-${info.versionName}.apk"
            )
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
    }
}
