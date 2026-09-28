package pl.edwin.budowlanka.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val changelog: String,
    val apkUrl: String
)

sealed class UpdateCheckResult {
    object UpToDate : UpdateCheckResult()
    data class Available(val info: UpdateInfo) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {
    const val manifestUrl =
        "https://raw.githubusercontent.com/edwinkarolczyk/Budowlanka/development-0.5/updates/manifest.json"

    suspend fun checkResult(context: Context): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val c = (URL(manifestUrl + "?t=" + System.currentTimeMillis()).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
            }
            connection = c

            val responseCode = c.responseCode
            if (responseCode !in 200..299) {
                return@withContext UpdateCheckResult.Error(
                    "Serwer aktualizacji zwrócił HTTP $responseCode."
                )
            }

            val body = c.inputStream.bufferedReader().use { it.readText() }
            val j = JSONObject(body)
            val remote = j.getInt("versionCode")
            val current = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .longVersionCode
                .toInt()

            if (remote > current) {
                UpdateCheckResult.Available(
                    UpdateInfo(
                        versionCode = remote,
                        versionName = j.getString("versionName"),
                        changelog = j.optString("changelog"),
                        apkUrl = j.getString("apkUrl")
                    )
                )
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (_: SocketTimeoutException) {
            UpdateCheckResult.Error("Przekroczono czas oczekiwania na serwer aktualizacji.")
        } catch (_: IOException) {
            UpdateCheckResult.Error("Brak połączenia z internetem lub serwerem aktualizacji.")
        } catch (_: Exception) {
            UpdateCheckResult.Error("Nie udało się odczytać informacji o aktualizacji.")
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun check(context: Context): UpdateInfo? =
        when (val result = checkResult(context)) {
            is UpdateCheckResult.Available -> result.info
            else -> null
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
