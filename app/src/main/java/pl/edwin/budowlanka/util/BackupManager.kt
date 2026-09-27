package pl.edwin.budowlanka.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import pl.edwin.budowlanka.data.AppDao
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BackupManager {
    private fun obj(vararg p: Pair<String, Any?>) = JSONObject().apply {
        p.forEach { (k, v) -> put(k, v ?: JSONObject.NULL) }
    }
    private fun arr(items: List<JSONObject>) = JSONArray(items)

    suspend fun shareBackup(context: Context, dao: AppDao) {
        val root = JSONObject()
        root.put("format", "budowlanka-backup-0.5")
        root.put("createdAt", System.currentTimeMillis())
        dao.getSettings()?.let { s ->
            root.put("settings", obj(
                "companyName" to s.companyName, "nip" to s.nip, "address" to s.address,
                "phone" to s.phone, "email" to s.email, "logoUri" to s.logoUri,
                "monthlyTarget" to s.monthlyTarget, "workDaysMonth" to s.workDaysMonth,
                "hoursPerDay" to s.hoursPerDay, "defaultKmRate" to s.defaultKmRate,
                "defaultFixedTravelFee" to s.defaultFixedTravelFee
            ))
        }
        root.put("works", arr(dao.allWorks().map { w -> obj(
            "id" to w.id, "name" to w.name, "category" to w.category, "unit" to w.unit,
            "laborRate" to w.laborRate, "laborRateLow" to w.laborRateLow,
            "laborRateHigh" to w.laborRateHigh, "laborHoursPerUnit" to w.laborHoursPerUnit,
            "defaultWastePct" to w.defaultWastePct, "priceRegion" to w.priceRegion,
            "priceYear" to w.priceYear, "priceSource" to w.priceSource,
            "includesMaterial" to w.includesMaterial, "isFavorite" to w.isFavorite,
            "isUserDefined" to w.isUserDefined, "active" to w.active
        ) }))
        root.put("materials", arr(dao.allMaterials().map { m -> obj(
            "id" to m.id, "name" to m.name, "unit" to m.unit, "manufacturer" to m.manufacturer,
            "priceBudget" to m.priceBudget, "priceStandard" to m.priceStandard,
            "pricePremium" to m.pricePremium, "stockQty" to m.stockQty, "active" to m.active
        ) }))
        root.put("workMaterials", arr(dao.allWorkMaterials().map { x -> obj(
            "workId" to x.workId, "materialId" to x.materialId, "qtyPerWorkUnit" to x.qtyPerWorkUnit
        ) }))
        root.put("tools", arr(dao.allTools().map { t -> obj(
            "id" to t.id, "name" to t.name, "code" to t.code, "notes" to t.notes, "active" to t.active
        ) }))
        root.put("workTools", arr(dao.allWorkTools().map { x -> obj(
            "workId" to x.workId, "toolId" to x.toolId, "qty" to x.qty
        ) }))
        root.put("packages", arr(dao.allPackages().map { p -> obj(
            "id" to p.id, "name" to p.name, "description" to p.description
        ) }))
        root.put("packageWorks", arr(dao.allPackageWorks().map { x -> obj(
            "packageId" to x.packageId, "workId" to x.workId, "position" to x.position
        ) }))
        root.put("clients", arr(dao.allClients().map { c -> obj(
            "id" to c.id, "name" to c.name, "phone" to c.phone, "email" to c.email,
            "notes" to c.notes, "loyaltyDiscountPct" to c.loyaltyDiscountPct
        ) }))
        root.put("sites", arr(dao.allSites().map { s -> obj(
            "id" to s.id, "clientId" to s.clientId, "name" to s.name,
            "address" to s.address, "notes" to s.notes
        ) }))
        root.put("estimates", arr(dao.allEstimates().map { e -> obj(
            "id" to e.id, "title" to e.title, "clientId" to e.clientId, "siteId" to e.siteId,
            "status" to e.status, "createdAt" to e.createdAt, "startDate" to e.startDate,
            "endDate" to e.endDate, "includeMaterials" to e.includeMaterials,
            "marginLaborPct" to e.marginLaborPct, "marginMaterialPct" to e.marginMaterialPct,
            "marginOverallPct" to e.marginOverallPct, "discountPct" to e.discountPct,
            "oneWayKm" to e.oneWayKm, "travelDays" to e.travelDays, "kmRate" to e.kmRate,
            "fixedTravelFee" to e.fixedTravelFee, "monthlyTargetSnapshot" to e.monthlyTargetSnapshot,
            "workDaysMonthSnapshot" to e.workDaysMonthSnapshot, "hoursDaySnapshot" to e.hoursDaySnapshot,
            "notes" to e.notes, "signatureData" to e.signatureData
        ) }))
        root.put("spaces", arr(dao.allSpaces().map { s -> obj(
            "id" to s.id, "estimateId" to s.estimateId, "level" to s.level, "name" to s.name,
            "length" to s.length, "width" to s.width, "height" to s.height, "openingsArea" to s.openingsArea
        ) }))
        root.put("openings", arr(dao.allOpenings().map { o -> obj(
            "id" to o.id, "spaceId" to o.spaceId, "type" to o.type, "name" to o.name,
            "width" to o.width, "height" to o.height, "quantity" to o.quantity
        ) }))
        root.put("estimateWorks", arr(dao.allEstimateWorks().map { x -> obj(
            "id" to x.id, "estimateId" to x.estimateId, "workId" to x.workId, "spaceId" to x.spaceId,
            "quantity" to x.quantity, "quantitySource" to x.quantitySource,
            "materialTier" to x.materialTier, "wastePctOverride" to x.wastePctOverride,
            "laborRateOverride" to x.laborRateOverride, "laborPriceMode" to x.laborPriceMode
        ) }))
        root.put("crewMembers", arr(dao.allCrewMembers().map { c -> obj(
            "id" to c.id, "name" to c.name, "defaultPayMode" to c.defaultPayMode,
            "defaultRate" to c.defaultRate, "active" to c.active
        ) }))
        root.put("estimateCrew", arr(dao.allEstimateCrew().map { c -> obj(
            "estimateId" to c.estimateId, "crewMemberId" to c.crewMemberId,
            "workSharePct" to c.workSharePct, "profitSharePct" to c.profitSharePct,
            "payMode" to c.payMode, "rate" to c.rate
        ) }))
        root.put("extraCosts", arr(dao.allExtraCosts().map { x -> obj(
            "id" to x.id, "estimateId" to x.estimateId, "category" to x.category,
            "description" to x.description, "amount" to x.amount
        ) }))
        root.put("shopping", arr(dao.allShopping().map { x -> obj(
            "estimateId" to x.estimateId, "materialId" to x.materialId,
            "requiredQty" to x.requiredQty, "status" to x.status
        ) }))
        root.put("toolChecklist", arr(dao.allToolChecklist().map { x -> obj(
            "estimateId" to x.estimateId, "toolId" to x.toolId, "qty" to x.qty, "packed" to x.packed
        ) }))
        root.put("priceHistory", arr(dao.allPriceHistory().map { x -> obj(
            "id" to x.id, "materialId" to x.materialId, "changedAt" to x.changedAt,
            "priceBudget" to x.priceBudget, "priceStandard" to x.priceStandard,
            "pricePremium" to x.pricePremium
        ) }))
        val photos = dao.allPhotos()
        root.put("photos", arr(photos.map { x -> obj(
            "id" to x.id, "estimateId" to x.estimateId, "spaceId" to x.spaceId,
            "estimateWorkId" to x.estimateWorkId, "uri" to x.uri, "caption" to x.caption
        ) }))

        val zip = File(context.cacheDir, "Budowlanka_backup_${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(zip)).use { zos ->
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(root.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            photos.forEach { p ->
                runCatching {
                    context.contentResolver.openInputStream(android.net.Uri.parse(p.uri))?.use { input ->
                        zos.putNextEntry(ZipEntry("photos/photo_${p.id}.bin"))
                        input.copyTo(zos)
                        zos.closeEntry()
                    }
                }
            }
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Zapisz / udostępnij kopię ZIP"))
    }
}
