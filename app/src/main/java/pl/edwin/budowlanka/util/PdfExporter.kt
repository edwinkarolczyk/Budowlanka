package pl.edwin.budowlanka.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import pl.edwin.budowlanka.data.EstimateEntity
import pl.edwin.budowlanka.domain.EstimateResult
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

data class PdfPayload(
    val companyName: String,
    val nip: String,
    val companyAddress: String,
    val companyPhone: String,
    val companyEmail: String,
    val clientName: String,
    val siteAddress: String,
    val estimate: EstimateEntity,
    val result: EstimateResult,
    val workLines: List<String>,
    val materialLines: List<String>,
    val crewLines: List<String>,
    val extraLines: List<String>
)

object PdfExporter {
    private val pl = Locale("pl", "PL")
    private fun money(v: Double): String =
        NumberFormat.getCurrencyInstance(pl).format(v)

    fun shareEstimate(context: Context, payload: PdfPayload, internal: Boolean) {
        val document = PdfDocument()
        val width = 595
        val height = 842
        val title = if (internal) "KOSZTORYS WEWNĘTRZNY" else "OFERTA / WYCENA"
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            isAntiAlias = true
        }
        val bold = Paint(paint).apply { typeface = android.graphics.Typeface.DEFAULT_BOLD }

        fun newPage() {
            page?.let { document.finishPage(it) }
            pageNo++
            page = document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNo).create())
            y = 42f
            page!!.canvas.drawText(title, 40f, y, Paint(bold).apply { textSize = 18f })
            y += 24f
        }
        fun line(text: String, isBold: Boolean = false, indent: Float = 0f) {
            if (page == null || y > 805f) newPage()
            page!!.canvas.drawText(text.take(105), 40f + indent, y, if (isBold) bold else paint)
            y += 17f
        }
        fun gap(v: Float = 8f) { y += v }

        newPage()
        line(payload.companyName.ifBlank { "Firma" }, true)
        if (payload.nip.isNotBlank()) line("NIP: ${payload.nip}")
        if (payload.companyAddress.isNotBlank()) line(payload.companyAddress)
        if (payload.companyPhone.isNotBlank()) line("tel. ${payload.companyPhone}")
        if (payload.companyEmail.isNotBlank()) line(payload.companyEmail)
        gap()

        line("Wycena: ${payload.estimate.title}", true)
        if (payload.clientName.isNotBlank()) line("Klient: ${payload.clientName}")
        if (payload.siteAddress.isNotBlank()) line("Inwestycja: ${payload.siteAddress}")
        if (payload.estimate.startDate.isNotBlank()) {
            line("Termin: ${payload.estimate.startDate} – ${payload.estimate.endDate.ifBlank { "do ustalenia" }}")
        }
        line("Status: ${payload.estimate.status}")
        gap()

        line("Zakres robót", true)
        payload.workLines.ifEmpty { listOf("Brak pozycji") }.forEach { line("• $it", indent = 8f) }
        gap()

        if (payload.estimate.includeMaterials && payload.materialLines.isNotEmpty()) {
            line("Materiały", true)
            payload.materialLines.forEach { line("• $it", indent = 8f) }
            gap()
        }

        if (!internal) {
            line("Razem dla klienta: ${money(payload.result.clientTotal)}", true)
            if (payload.result.discountValue > 0.0) {
                line("Uwzględniony rabat: ${money(payload.result.discountValue)}")
            }
            if (payload.estimate.notes.isNotBlank()) {
                gap(); line("Uwagi", true); line(payload.estimate.notes)
            }
        } else {
            line("Rozliczenie wewnętrzne", true)
            line("Robocizna bazowa: ${money(payload.result.laborBase)}")
            line("Materiały koszt: ${money(payload.result.materialBase)}")
            line("Dojazd: ${money(payload.result.travelCost)}")
            line("Koszty dodatkowe: ${money(payload.result.extraCosts)}")
            line("Koszt ekipy (stawki): ${money(payload.result.crewDirectCost)}")
            line("Cena przed rabatem: ${money(payload.result.beforeDiscount)}")
            line("Rabat: ${money(payload.result.discountValue)}")
            line("Cena klienta: ${money(payload.result.clientTotal)}", true)
            line("Szacowany zysk przed podziałem procentowym: ${money(payload.result.estimatedProfitBeforeProfitShare)}", true)
            line("Roboczogodziny: ${"%.1f".format(pl, payload.result.laborHours)} h")
            line("Termin techniczny: ${"%.1f".format(pl, payload.result.technicalDays)} dni")
            line("Maks. termin finansowy: ${"%.1f".format(pl, payload.result.financialMaxDays)} dni")
            gap()
            if (payload.crewLines.isNotEmpty()) {
                line("Ekipa", true)
                payload.crewLines.forEach { line("• $it", indent = 8f) }
            }
            if (payload.extraLines.isNotEmpty()) {
                gap(); line("Koszty dodatkowe", true)
                payload.extraLines.forEach { line("• $it", indent = 8f) }
            }
        }

        if (payload.estimate.signatureData.isNotBlank()) {
            gap(14f)
            line("Podpis klienta", true)
            val pts = payload.estimate.signatureData.split("|").mapNotNull {
                val p = it.split(",")
                if (p.size == 2) {
                    val x = p[0].toFloatOrNull()
                    val yy = p[1].toFloatOrNull()
                    if (x != null && yy != null) x to yy else null
                } else null
            }
            if (pts.size > 1 && page != null) {
                val sigPaint = Paint().apply {
                    color = android.graphics.Color.BLACK
                    strokeWidth = 1.8f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                val maxX = (pts.maxOfOrNull { it.first } ?: 1f).coerceAtLeast(1f)
                val maxY = (pts.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
                val scale = minOf(220f / maxX, 80f / maxY)
                var prev = pts.first()
                pts.drop(1).forEach { cur ->
                    page!!.canvas.drawLine(
                        45f + prev.first * scale, y + prev.second * scale,
                        45f + cur.first * scale, y + cur.second * scale,
                        sigPaint
                    )
                    prev = cur
                }
                y += 90f
            }
        }

        page?.let { document.finishPage(it) }

        val safe = payload.estimate.title.replace(Regex("[^A-Za-z0-9ąćęłńóśżźĄĆĘŁŃÓŚŻŹ_-]"), "_")
        val file = File(context.cacheDir, (if (internal) "wewnetrzny_" else "oferta_") + safe + ".pdf")
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Udostępnij PDF"))
    }
}
