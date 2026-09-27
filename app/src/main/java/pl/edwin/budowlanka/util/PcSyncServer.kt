package pl.edwin.budowlanka.util

import android.content.Context
import kotlinx.coroutines.runBlocking
import pl.edwin.budowlanka.data.AppDao
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

data class PcSyncStatus(
    val running: Boolean = false,
    val url: String = "",
    val code: String = "",
    val error: String = ""
)

object PcSyncServer {
    private const val PORT = 8787
    private const val PREFS = "pc_sync"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_CODE = "pair_code"

    @Volatile private var socket: ServerSocket? = null
    @Volatile private var thread: Thread? = null
    @Volatile private var state = PcSyncStatus()

    fun currentStatus(): PcSyncStatus = state

    fun shouldAutoStart(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)

    @Synchronized
    fun start(context: Context, dao: AppDao): PcSyncStatus {
        if (socket?.isClosed == false) return state

        return runCatching {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val code = prefs.getString(KEY_CODE, null) ?: generateCode().also {
                prefs.edit().putString(KEY_CODE, it).apply()
            }
            val html = app.assets.open("pc/index.html").bufferedReader().use { it.readText() }

            val server = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(PORT))
            }
            socket = server
            state = PcSyncStatus(
                running = true,
                url = "http://" + localIpv4() + ":" + PORT + "/",
                code = code
            )
            prefs.edit().putBoolean(KEY_ENABLED, true).apply()

            thread = Thread({
                while (!server.isClosed) {
                    val client = try { server.accept() } catch (_: Throwable) { break }
                    runCatching { handle(client, dao, code, html) }
                    runCatching { client.close() }
                }
            }, "Budowlanka-PC-Sync").apply {
                isDaemon = true
                start()
            }
            state
        }.getOrElse { err ->
            runCatching { socket?.close() }
            socket = null
            thread = null
            state = PcSyncStatus(error = err.message ?: "Nie udało się uruchomić synchronizacji.")
            state
        }
    }

    @Synchronized
    fun stop(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, false).apply()
        runCatching { socket?.close() }
        socket = null
        thread = null
        state = PcSyncStatus()
    }

    private fun handle(client: Socket, dao: AppDao, code: String, html: String) {
        client.soTimeout = 5000
        val reader = client.getInputStream().bufferedReader(StandardCharsets.UTF_8)
        val first = reader.readLine() ?: return
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) break
        }

        val parts = first.split(" ")
        if (parts.size < 2 || parts[0] != "GET") {
            respond(client, 405, "text/plain; charset=utf-8", "Tylko odczyt")
            return
        }

        val uri = runCatching { URI(parts[1]) }.getOrNull()
        when (uri?.path ?: "/") {
            "/", "/index.html" -> respond(client, 200, "text/html; charset=utf-8", html)
            "/health" -> respond(client, 200, "application/json; charset=utf-8", """{"ok":true,"readOnly":true}""")
            "/snapshot" -> {
                if (query(uri?.rawQuery.orEmpty(), "code") != code) {
                    respond(client, 403, "application/json; charset=utf-8", """{"error":"Nieprawidłowy kod"}""")
                    return
                }
                val body = runBlocking { BackupManager.buildSnapshot(dao).toString() }
                respond(client, 200, "application/json; charset=utf-8", body)
            }
            else -> respond(client, 404, "text/plain; charset=utf-8", "Brak zasobu")
        }
    }

    private fun respond(client: Socket, status: Int, type: String, body: String) {
        val reason = when (status) {
            200 -> "OK"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            else -> "OK"
        }
        val data = body.toByteArray(StandardCharsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 ").append(status).append(" ").append(reason).append("\r\n")
            append("Content-Type: ").append(type).append("\r\n")
            append("Content-Length: ").append(data.size).append("\r\n")
            append("Cache-Control: no-store\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        client.getOutputStream().use { out ->
            out.write(header)
            out.write(data)
            out.flush()
        }
    }

    private fun query(raw: String, key: String): String? =
        raw.split("&").mapNotNull { item ->
            val i = item.indexOf('=')
            if (i <= 0) null else
                URLDecoder.decode(item.substring(0, i), "UTF-8") to
                    URLDecoder.decode(item.substring(i + 1), "UTF-8")
        }.firstOrNull { it.first == key }?.second

    private fun localIpv4(): String {
        val all = NetworkInterface.getNetworkInterfaces()
        while (all.hasMoreElements()) {
            val nic = all.nextElement()
            if (!nic.isUp || nic.isLoopback) continue
            val addresses = nic.inetAddresses
            while (addresses.hasMoreElements()) {
                val address = addresses.nextElement()
                if (address is Inet4Address && address.isSiteLocalAddress && !address.isLoopbackAddress) {
                    return address.hostAddress ?: continue
                }
            }
        }
        return "127.0.0.1"
    }

    private fun generateCode(): String =
        (SecureRandom().nextInt(900000) + 100000).toString()
}
