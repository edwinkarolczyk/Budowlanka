package pl.edwin.budowlanka.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class RouteResult(
    val oneWayKm: Double,
    val originLabel: String,
    val destinationLabel: String
)

object RouteCalculator {
    suspend fun calculate(origin: String, destination: String): Result<RouteResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(origin.isNotBlank()) { "Ustaw adres firmy w Ustawieniach." }
            require(destination.isNotBlank()) { "Brak adresu klienta / inwestycji." }

            val a = geocode(origin)
            val b = geocode(destination)

            val url = URL(
                "https://router.project-osrm.org/route/v1/driving/" +
                    "${a.second},${a.first};${b.second},${b.first}" +
                    "?overview=false&alternatives=false&steps=false"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 10000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Budowlanka/0.5 Android")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)
            val route = json.optJSONArray("routes")?.optJSONObject(0)
                ?: error("Nie udało się wyznaczyć trasy.")
            val meters = route.optDouble("distance", -1.0)
            require(meters >= 0.0) { "Brak odległości trasy." }

            RouteResult(
                oneWayKm = meters / 1000.0,
                originLabel = origin,
                destinationLabel = destination
            )
        }
    }

    private fun geocode(address: String): Pair<Double, Double> {
        val query = URLEncoder.encode(address, Charsets.UTF_8.name())
        val url = URL("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=$query")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10000
            readTimeout = 10000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Budowlanka/0.5 Android")
            setRequestProperty("Accept-Language", "pl")
        }
        val text = conn.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        require(arr.length() > 0) { "Nie znaleziono adresu: $address" }
        val obj = arr.getJSONObject(0)
        return obj.getString("lat").toDouble() to obj.getString("lon").toDouble()
    }
}
