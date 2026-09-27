package pl.edwin.budowlanka.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.edwin.budowlanka.MainViewModel
import pl.edwin.budowlanka.data.*
import pl.edwin.budowlanka.domain.EstimateCalculator
import pl.edwin.budowlanka.domain.EstimateResult
import pl.edwin.budowlanka.domain.SchedulePlanner
import pl.edwin.budowlanka.util.PdfExporter
import pl.edwin.budowlanka.util.PdfPayload
import kotlinx.coroutines.launch

@Composable
fun EstimateEditor(vm: MainViewModel, estimateId: Long, onBack: () -> Unit) {
    val estimates by vm.estimates.collectAsStateWithLifecycle()
    val works by vm.works.collectAsStateWithLifecycle()
    val materials by vm.materials.collectAsStateWithLifecycle()
    val workMaterials by vm.workMaterials.collectAsStateWithLifecycle()
    val tools by vm.tools.collectAsStateWithLifecycle()
    val workTools by vm.workTools.collectAsStateWithLifecycle()
    val packages by vm.packages.collectAsStateWithLifecycle()
    val packageWorks by vm.packageWorks.collectAsStateWithLifecycle()
    val clients by vm.clients.collectAsStateWithLifecycle()
    val sites by vm.sites.collectAsStateWithLifecycle()
    val allSpaces by vm.spaces.collectAsStateWithLifecycle()
    val allLines by vm.estimateWorks.collectAsStateWithLifecycle()
    val crewMembers by vm.crewMembers.collectAsStateWithLifecycle()
    val allCrew by vm.estimateCrew.collectAsStateWithLifecycle()
    val allExtras by vm.extraCosts.collectAsStateWithLifecycle()
    val photos by vm.photos.collectAsStateWithLifecycle()
    val shopping by vm.shopping.collectAsStateWithLifecycle()
    val toolChecklist by vm.toolChecklist.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    val estimate = estimates.firstOrNull { it.id == estimateId }
    if (estimate == null) {
        Box(Modifier.fillMaxSize().padding(20.dp)) {
            Column {
                Text("Ładowanie wyceny…")
                TextButton(onClick = onBack) { Text("Wróć") }
            }
        }
        return
    }

    val spaces = allSpaces.filter { it.estimateId == estimateId }
    val lines = allLines.filter { it.estimateId == estimateId }
    val crew = allCrew.filter { it.estimateId == estimateId }
    val extras = allExtras.filter { it.estimateId == estimateId }

    val result = remember(
        estimate, works, lines, materials, workMaterials, tools, workTools, spaces, crew, extras
    ) {
        EstimateCalculator.calculate(
            estimate, works, lines, materials, workMaterials, tools, workTools,
            spaces, crew, extras
        )
    }

    var tab by rememberSaveableCompat { mutableStateOf("Wycena") }

    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 4.dp) {
            Column(Modifier.fillMaxWidth().padding(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onBack) { Text("← Wróć") }
                    Column {
                        Text(estimate.title, style = MaterialTheme.typography.titleMedium)
                        Text(estimate.status, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(money(result.clientTotal), style = MaterialTheme.typography.titleMedium)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Wycena", "Pomiary", "Roboty", "Ekipa", "Realizacja").forEach {
                        TextButton(onClick = { tab = it }) {
                            Text(it, color = if (tab == it) MaterialTheme.colorScheme.primary else LocalContentColor.current)
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (tab) {
                "Wycena" -> EstimateOverview(vm, estimate, result, clients, sites, estimates, works, allLines, materials, workMaterials, tools, workTools, allSpaces, allCrew, allExtras)
                "Pomiary" -> MeasurementsTab(vm, estimate, spaces)
                "Roboty" -> WorksTab(vm, estimate, works, materials, packages, packageWorks, spaces, lines, result)
                "Ekipa" -> CrewTab(vm, estimate, crewMembers, crew, result)
                else -> RealizationTab(vm, estimate, result, works, lines, materials, clients, sites, crewMembers, crew, extras, spaces, photos.filter { it.estimateId == estimateId }, shopping.filter { it.estimateId == estimateId }, toolChecklist.filter { it.estimateId == estimateId }, tools, settings)
            }
        }
    }
}

@Composable
private fun EstimateOverview(
    vm: MainViewModel,
    e: EstimateEntity,
    result: EstimateResult,
    clients: List<ClientEntity>,
    sites: List<SiteEntity>,
    allEstimates: List<EstimateEntity>,
    works: List<WorkEntity>,
    allLines: List<EstimateWorkEntity>,
    materials: List<MaterialEntity>,
    workMaterials: List<WorkMaterialEntity>,
    tools: List<ToolEntity>,
    workTools: List<WorkToolEntity>,
    spaces: List<SpaceEntity>,
    allCrew: List<EstimateCrewEntity>,
    allExtras: List<ExtraCostEntity>
) {
    val client = clients.firstOrNull { it.id == e.clientId }
    val completedSpend = if (client == null) 0.0 else allEstimates
        .filter { it.clientId == client.id && it.status == EstimateStatus.DONE && it.id != e.id }
        .sumOf { old ->
            EstimateCalculator.calculate(
                old, works, allLines, materials, workMaterials, tools, workTools,
                spaces, allCrew, allExtras
            ).clientTotal
        }
    val historySuggestion = when {
        completedSpend >= 100000.0 -> 5.0
        completedSpend >= 50000.0 -> 3.0
        completedSpend >= 20000.0 -> 2.0
        else -> 0.0
    }
    val suggestedDiscount = maxOf(client?.loyaltyDiscountPct ?: 0.0, historySuggestion)

    var showExtra by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Section("Klient i zlecenie") {
            OutlinedTextField(
                e.title,
                { vm.saveEstimate(e.copy(title = it)) },
                label = { Text("Nazwa wyceny") },
                modifier = Modifier.fillMaxWidth()
            )
            SimpleDropdown(
                "Klient",
                (e.clientId ?: 0L).toString(),
                listOf("0" to "Bez klienta") + clients.map { it.id.toString() to it.name },
                {
                    val id = it.toLongOrNull()?.takeIf { v -> v != 0L }
                    vm.saveEstimate(e.copy(clientId = id, siteId = null))
                }
            )
            val clientSites = sites.filter { it.clientId == e.clientId }
            SimpleDropdown(
                "Adres / inwestycja",
                (e.siteId ?: 0L).toString(),
                listOf("0" to "Bez adresu") + clientSites.map { it.id.toString() to "${it.name} ${it.address}" },
                { vm.saveEstimate(e.copy(siteId = it.toLongOrNull()?.takeIf { v -> v != 0L })) }
            )
            SimpleDropdown("Status", e.status, EstimateStatus.all.map { it to it }, { vm.saveEstimate(e.copy(status = it)) })
            OutlinedTextField(e.startDate, { vm.saveEstimate(e.copy(startDate = it)) }, label = { Text("Start RRRR-MM-DD") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(e.endDate, { vm.saveEstimate(e.copy(endDate = it)) }, label = { Text("Koniec RRRR-MM-DD") }, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = {
                val p = SchedulePlanner.propose(e.id, result.technicalDays, allEstimates)
                vm.saveEstimate(e.copy(startDate = p.first, endDate = p.second))
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Zaproponuj pierwszy wolny termin")
            }
        }

        Section("Materiały i marża") {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(e.includeMaterials, { vm.saveEstimate(e.copy(includeMaterials = it)) })
                Text(if (e.includeMaterials) "Robocizna + materiały" else "Tylko robocizna")
            }
            Text("Klasa materiału jest wybierana osobno przy każdej robocie.")
            NumberField("Marża robocizny %", e.marginLaborPct, { vm.saveEstimate(e.copy(marginLaborPct = it)) }, Modifier.fillMaxWidth())
            NumberField("Marża materiałów %", e.marginMaterialPct, { vm.saveEstimate(e.copy(marginMaterialPct = it)) }, Modifier.fillMaxWidth())
            NumberField("Marża całego zlecenia %", e.marginOverallPct, { vm.saveEstimate(e.copy(marginOverallPct = it)) }, Modifier.fillMaxWidth())

            if (client != null) {
                Text("Historia zakończonych zleceń klienta: ${money(completedSpend)}")
                Text("Sugerowany rabat: $suggestedDiscount%")
                if (suggestedDiscount > 0) {
                    OutlinedButton(onClick = { vm.saveEstimate(e.copy(discountPct = suggestedDiscount)) }) {
                        Text("Zastosuj sugerowany rabat")
                    }
                }
            }
            NumberField("Rabat własny %", e.discountPct, { vm.saveEstimate(e.copy(discountPct = it.coerceIn(0.0, 100.0))) }, Modifier.fillMaxWidth())
            Text("Wartość rabatu: ${money(result.discountValue)}")
        }

        Section("Dojazd") {
            NumberField("Km w jedną stronę", e.oneWayKm, { vm.saveEstimate(e.copy(oneWayKm = it)) }, Modifier.fillMaxWidth())
            IntField("Liczba dni przejazdu", e.travelDays, { vm.saveEstimate(e.copy(travelDays = it)) }, Modifier.fillMaxWidth())
            NumberField("Stawka za km", e.kmRate, { vm.saveEstimate(e.copy(kmRate = it)) }, Modifier.fillMaxWidth())
            NumberField("Stała kwota dojazdu", e.fixedTravelFee, { vm.saveEstimate(e.copy(fixedTravelFee = it)) }, Modifier.fillMaxWidth())
            Text("A↔B × dni + stała opłata = ${money(result.travelCost)}")
        }

        Section("Opłacalność i termin") {
            ResultSummary(result)
            if (result.technicalDays > result.financialMaxDays && result.financialMaxDays > 0.0) {
                Text("⚠ Techniczny czas przekracza termin wynikający z celu dochodowego.", color = MaterialTheme.colorScheme.error)
            } else if (result.financialMaxDays > 0.0) {
                Text("✓ Zlecenie mieści się w założeniu dochodowym.")
            }
        }

        Section("Koszty dodatkowe") {
            allExtras.filter { it.estimateId == e.id }.forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${it.category}: ${it.description}")
                    Text(money(it.amount))
                }
            }
            Button(onClick = { showExtra = true }) { Text("+ Koszt dodatkowy") }
        }

        Section("Uwagi") {
            OutlinedTextField(
                e.notes,
                { vm.saveEstimate(e.copy(notes = it)) },
                label = { Text("Uwagi do zlecenia") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }

    if (showExtra) {
        var category by remember { mutableStateOf("Inne") }
        var desc by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf(0.0) }
        AlertDialog(
            onDismissRequest = { showExtra = false },
            title = { Text("Koszt dodatkowy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(category, { category = it }, label = { Text("Kategoria, np. gruz / rusztowanie") })
                    OutlinedTextField(desc, { desc = it }, label = { Text("Opis") })
                    NumberField("Kwota", amount, { amount = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addExtra(ExtraCostEntity(estimateId = e.id, category = category, description = desc.ifBlank { category }, amount = amount))
                    showExtra = false
                }) { Text("Dodaj") }
            },
            dismissButton = { TextButton(onClick = { showExtra = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun ResultSummary(r: EstimateResult) {
    Text("Cena dla klienta: ${money(r.clientTotal)}", style = MaterialTheme.typography.headlineSmall)
    Text("Robocizna bazowa: ${money(r.laborBase)}")
    Text("Materiały: ${money(r.materialBase)}")
    Text("Koszt ekipy wg stawek: ${money(r.crewDirectCost)}")
    Text("Szacowany zysk przed podziałem %: ${money(r.estimatedProfitBeforeProfitShare)}")
    Text("Roboczogodziny: ${"%.1f".format(r.laborHours)} h")
    Text("Termin techniczny: ${"%.1f".format(r.technicalDays)} dni")
    Text("Termin finansowy maks.: ${"%.1f".format(r.financialMaxDays)} dni")
    Text("Cel na dzień: ${money(r.targetPerDay)}")
}

@Composable
private fun MeasurementsTab(vm: MainViewModel, e: EstimateEntity, spaces: List<SpaceEntity>) {
    var showAdd by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Pomieszczenia / strefy", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showAdd = true }) { Text("+ Pomieszczenie") }
        }
        Text("Okna i drzwi wpisujesz jako łączną powierzchnię otworów — są odejmowane od ścian.")

        spaces.forEach { s ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("${s.level} • ${s.name}", style = MaterialTheme.typography.titleMedium)
                    Text("${s.length} × ${s.width} × h ${s.height} m")
                    Text("Podłoga/sufit: ${"%.2f".format(s.floorArea())} m²")
                    Text("Ściany po odjęciu otworów: ${"%.2f".format(s.wallArea())} m²")
                    Text("Otwory: ${s.openingsArea} m²")
                    TextButton(onClick = { vm.deleteSpace(s) }) { Text("Usuń") }
                }
            }
        }
    }

    if (showAdd) {
        var level by remember { mutableStateOf("Parter") }
        var name by remember { mutableStateOf("") }
        var l by remember { mutableStateOf(0.0) }
        var w by remember { mutableStateOf(0.0) }
        var h by remember { mutableStateOf(2.6) }
        var openings by remember { mutableStateOf(0.0) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Pomieszczenie") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(level, { level = it }, label = { Text("Poziom / strefa") })
                    OutlinedTextField(name, { name = it }, label = { Text("Nazwa") })
                    NumberField("Długość [m]", l, { l = it })
                    NumberField("Szerokość [m]", w, { w = it })
                    NumberField("Wysokość [m]", h, { h = it })
                    NumberField("Okna + drzwi [m²]", openings, { openings = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addSpace(SpaceEntity(estimateId = e.id, level = level, name = name.ifBlank { "Pomieszczenie" }, length = l, width = w, height = h, openingsArea = openings))
                    showAdd = false
                }) { Text("Dodaj") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun WorksTab(
    vm: MainViewModel,
    e: EstimateEntity,
    works: List<WorkEntity>,
    materials: List<MaterialEntity>,
    packages: List<PackageEntity>,
    packageWorks: List<PackageWorkEntity>,
    spaces: List<SpaceEntity>,
    lines: List<EstimateWorkEntity>,
    result: EstimateResult
) {
    var add by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<EstimateWorkEntity?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Żywy zakres robót", style = MaterialTheme.typography.headlineSmall)
        Text("Dodajesz lub usuwasz pozycję — cena, czas, materiały i zakupy przeliczają się od razu.")

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { add = true }) { Text("+ Robota") }
        }

        if (packages.isNotEmpty()) {
            Section("Pakiety") {
                packages.forEach { p ->
                    OutlinedButton(onClick = { vm.addPackageToEstimate(e.id, p.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text("+ ${p.name}")
                    }
                }
            }
        }

        lines.forEach { line ->
            val work = works.firstOrNull { it.id == line.workId } ?: return@forEach
            val qty = EstimateCalculator.resolveQuantity(line, spaces)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(work.name, style = MaterialTheme.typography.titleMedium)
                    Text("${"%.2f".format(qty)} ${work.unit} • ${line.quantitySource}")
                    Text("Materiał: ${line.materialTier} • zapas ${line.wastePctOverride ?: work.defaultWastePct}%")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { edit = line }) { Text("Edytuj") }
                        TextButton(onClick = { vm.deleteEstimateWork(line) }) { Text("Usuń z zakresu") }
                    }
                }
            }
        }

        Section("Podsumowanie na żywo") { ResultSummary(result) }

        if (result.materials.isNotEmpty()) {
            Section("Szacowane materiały") {
                result.materials.forEach {
                    Text("• ${it.name}: ${"%.2f".format(it.quantity)} ${it.unit} = ${money(it.cost)}")
                }
            }
        }
    }

    if (add) {
        WorkLineDialog(e.id, null, works, spaces, onDismiss = { add = false }) {
            vm.addEstimateWork(it); add = false
        }
    }
    edit?.let { current ->
        WorkLineDialog(e.id, current, works, spaces, onDismiss = { edit = null }) {
            vm.addEstimateWork(it); edit = null
        }
    }
}

@Composable
private fun WorkLineDialog(
    estimateId: Long,
    current: EstimateWorkEntity?,
    works: List<WorkEntity>,
    spaces: List<SpaceEntity>,
    onDismiss: () -> Unit,
    onSave: (EstimateWorkEntity) -> Unit
) {
    var workId by remember { mutableStateOf(current?.workId ?: works.firstOrNull()?.id ?: 0L) }
    var spaceId by remember { mutableStateOf(current?.spaceId ?: 0L) }
    var source by remember { mutableStateOf(current?.quantitySource ?: QuantitySource.MANUAL) }
    var qty by remember { mutableStateOf(current?.quantity ?: 1.0) }
    var tier by remember { mutableStateOf(current?.materialTier ?: MaterialTier.STANDARD) }
    var waste by remember { mutableStateOf(current?.wastePctOverride ?: -1.0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == null) "Dodaj robotę" else "Edytuj robotę") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleDropdown("Robota", workId.toString(), works.map { it.id.toString() to it.name }) {
                    workId = it.toLongOrNull() ?: 0L
                }
                SimpleDropdown("Pomieszczenie", spaceId.toString(), listOf("0" to "Brak / całe zlecenie") + spaces.map { it.id.toString() to "${it.level} • ${it.name}" }) {
                    spaceId = it.toLongOrNull() ?: 0L
                }
                SimpleDropdown("Ilość z", source, QuantitySource.all.map { it to it }) { source = it }
                NumberField("Ilość ręczna", qty, { qty = it })
                SimpleDropdown("Klasa materiału", tier, MaterialTier.all.map { it to it }) { tier = it }
                NumberField("Zapas % (-1 = domyślny roboty)", waste, { waste = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    EstimateWorkEntity(
                        id = current?.id ?: 0L,
                        estimateId = estimateId,
                        workId = workId,
                        spaceId = spaceId.takeIf { it != 0L },
                        quantity = qty,
                        quantitySource = source,
                        materialTier = tier,
                        wastePctOverride = waste.takeIf { it >= 0.0 }
                    )
                )
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun CrewTab(
    vm: MainViewModel,
    e: EstimateEntity,
    members: List<CrewMemberEntity>,
    crew: List<EstimateCrewEntity>,
    result: EstimateResult
) {
    var add by remember { mutableStateOf(false) }
    val workSum = crew.sumOf { it.workSharePct }
    val profitSum = crew.filter { it.payMode == PayMode.PROFIT_PERCENT }.sumOf { it.profitSharePct }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Ekipa 1–6 osób", style = MaterialTheme.typography.headlineSmall)
        Text("Udział w pracy i udział w zysku są osobne. Stawka godzinowa/dzienna/stała jest kosztem przed podziałem zysku.")
        Text("Suma udziału pracy: $workSum%")
        Text("Suma udziałów procentowych zysku: $profitSum%")
        Button(onClick = { add = true }, enabled = crew.size < 6) { Text("+ Osoba do zlecenia") }

        crew.forEach { c ->
            val m = members.firstOrNull { it.id == c.crewMemberId } ?: return@forEach
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(m.name, style = MaterialTheme.typography.titleMedium)
                    NumberField("Udział w pracy %", c.workSharePct, { vm.assignCrew(c.copy(workSharePct = it)) }, Modifier.fillMaxWidth())
                    SimpleDropdown("Rozliczenie", c.payMode, PayMode.all.map { it to it }) { vm.assignCrew(c.copy(payMode = it)) }
                    if (c.payMode == PayMode.PROFIT_PERCENT) {
                        NumberField("Udział w zysku %", c.profitSharePct, { vm.assignCrew(c.copy(profitSharePct = it)) }, Modifier.fillMaxWidth())
                        Text("Szacunek udziału: ${money(result.estimatedProfitBeforeProfitShare * c.profitSharePct / 100.0)}")
                    } else {
                        NumberField("Stawka", c.rate, { vm.assignCrew(c.copy(rate = it)) }, Modifier.fillMaxWidth())
                    }
                    TextButton(onClick = { vm.removeCrew(e.id, c.crewMemberId) }) { Text("Usuń z ekipy") }
                }
            }
        }
        Section("Czas") {
            Text("Roboczogodziny: ${"%.1f".format(result.laborHours)} h")
            Text("Szacowany termin techniczny: ${"%.1f".format(result.technicalDays)} dni")
        }
    }

    if (add) {
        val available = members.filter { m -> crew.none { it.crewMemberId == m.id } }
        var memberId by remember { mutableStateOf(available.firstOrNull()?.id ?: 0L) }
        var workShare by remember { mutableStateOf(if (crew.isEmpty()) 100.0 else 0.0) }
        AlertDialog(
            onDismissRequest = { add = false },
            title = { Text("Osoba do zlecenia") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimpleDropdown("Osoba", memberId.toString(), available.map { it.id.toString() to it.name }) {
                        memberId = it.toLongOrNull() ?: 0L
                    }
                    NumberField("Udział w pracy %", workShare, { workShare = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val m = members.firstOrNull { it.id == memberId }
                    if (m != null) {
                        vm.assignCrew(EstimateCrewEntity(e.id, m.id, workSharePct = workShare, profitSharePct = if (m.defaultPayMode == PayMode.PROFIT_PERCENT) m.defaultRate else 0.0, payMode = m.defaultPayMode, rate = if (m.defaultPayMode == PayMode.PROFIT_PERCENT) 0.0 else m.defaultRate))
                    }
                    add = false
                }) { Text("Dodaj") }
            },
            dismissButton = { TextButton(onClick = { add = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun RealizationTab(
    vm: MainViewModel,
    e: EstimateEntity,
    result: EstimateResult,
    works: List<WorkEntity>,
    lines: List<EstimateWorkEntity>,
    materials: List<MaterialEntity>,
    clients: List<ClientEntity>,
    sites: List<SiteEntity>,
    crewMembers: List<CrewMemberEntity>,
    crew: List<EstimateCrewEntity>,
    extras: List<ExtraCostEntity>,
    spaces: List<SpaceEntity>,
    photos: List<PhotoEntity>,
    shopping: List<ShoppingItemEntity>,
    checklist: List<ToolChecklistEntity>,
    tools: List<ToolEntity>,
    settings: AppSettingsEntity?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val active = e.status == EstimateStatus.ACCEPTED || e.status == EstimateStatus.IN_PROGRESS || e.status == EstimateStatus.DONE
    var photoSpaceId by remember { mutableStateOf(0L) }
    var photoWorkId by remember { mutableStateOf(0L) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            vm.addPhoto(
                PhotoEntity(
                    estimateId = e.id,
                    spaceId = photoSpaceId.takeIf { it != 0L },
                    estimateWorkId = photoWorkId.takeIf { it != 0L },
                    uri = uri.toString()
                )
            )
        }
    }

    val payload = remember(e, result, lines, works, crew, crewMembers, extras, settings) {
        PdfPayload(
            companyName = settings?.companyName.orEmpty(),
            nip = settings?.nip.orEmpty(),
            companyAddress = settings?.address.orEmpty(),
            companyPhone = settings?.phone.orEmpty(),
            companyEmail = settings?.email.orEmpty(),
            clientName = clients.firstOrNull { it.id == e.clientId }?.name.orEmpty(),
            siteAddress = sites.firstOrNull { it.id == e.siteId }?.address.orEmpty(),
            estimate = e,
            result = result,
            workLines = lines.mapNotNull { line ->
                works.firstOrNull { it.id == line.workId }?.let { w ->
                    "${w.name}: ${"%.2f".format(EstimateCalculator.resolveQuantity(line, spaces))} ${w.unit}"
                }
            },
            materialLines = result.materials.map { "${it.name}: ${"%.2f".format(it.quantity)} ${it.unit}" },
            crewLines = crew.mapNotNull { c ->
                crewMembers.firstOrNull { it.id == c.crewMemberId }?.let { m ->
                    "${m.name}: praca ${c.workSharePct}%, ${c.payMode} ${c.rate}"
                }
            },
            extraLines = extras.map { "${it.category}: ${it.description} — ${money(it.amount)}" }
        )
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Realizacja", style = MaterialTheme.typography.headlineSmall)

        Section("Dokumenty") {
            Button(onClick = { PdfExporter.shareEstimate(context, payload, internal = false) }, modifier = Modifier.fillMaxWidth()) {
                Text("PDF dla klienta")
            }
            OutlinedButton(onClick = { PdfExporter.shareEstimate(context, payload, internal = true) }, modifier = Modifier.fillMaxWidth()) {
                Text("PDF wewnętrzny")
            }
        }

        if (!active) {
            Section("Po akceptacji") {
                Text("Ustaw status „ZAAKCEPTOWANA”. Aplikacja utworzy listę zakupów i listę narzędzi do spakowania.")
            }
        } else {
            Section("Lista zakupów") {
                OutlinedButton(onClick = { scope.launch { vm.syncFulfillment(e.id) } }) {
                    Text("Odśwież listy z aktualnego zakresu")
                }
                if (shopping.isEmpty()) Text("Brak materiałów albo lista nie została jeszcze utworzona.")
                shopping.forEach { item ->
                    val m = materials.firstOrNull { it.id == item.materialId }
                    if (m != null) {
                        Text("${m.name}: ${"%.2f".format(item.requiredQty)} ${m.unit}")
                        SimpleDropdown(
                            "Stan",
                            item.status,
                            FulfillmentStatus.all.map { it to it },
                            { vm.setShoppingStatus(item, it) }
                        )
                    }
                }
            }

            Section("Co zabrać na adres") {
                if (checklist.isEmpty()) Text("Brak przypisanych narzędzi.")
                checklist.forEach { item ->
                    val t = tools.firstOrNull { it.id == item.toolId }
                    if (t != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(item.packed, { vm.setToolPacked(item, it) })
                                Text("${t.name} × ${item.qty}")
                            }
                            Text(if (item.packed) "spakowane" else "do zabrania")
                        }
                    }
                }
            }
        }

        Section("Zdjęcia z oględzin / budowy") {
            SimpleDropdown(
                "Pomieszczenie",
                photoSpaceId.toString(),
                listOf("0" to "Bez pomieszczenia") + spaces.map { it.id.toString() to it.name },
                {
                    photoSpaceId = it.toLongOrNull() ?: 0L
                    if (photoSpaceId != 0L) photoWorkId = 0L
                }
            )
            SimpleDropdown(
                "albo konkretna robota",
                photoWorkId.toString(),
                listOf("0" to "Bez roboty") + lines.mapNotNull { line ->
                    works.firstOrNull { it.id == line.workId }?.let { w -> line.id.toString() to w.name }
                },
                {
                    photoWorkId = it.toLongOrNull() ?: 0L
                    if (photoWorkId != 0L) photoSpaceId = 0L
                }
            )
            Button(onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("+ Dodaj zdjęcie") }
            photos.forEach { p ->
                val place = when {
                    p.spaceId != null -> spaces.firstOrNull { it.id == p.spaceId }?.name ?: "pomieszczenie"
                    p.estimateWorkId != null -> {
                        val line = lines.firstOrNull { it.id == p.estimateWorkId }
                        works.firstOrNull { it.id == line?.workId }?.name ?: "robota"
                    }
                    else -> "całe zlecenie"
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Zdjęcie #${p.id} • $place")
                    TextButton(onClick = { vm.deletePhoto(p) }) { Text("Usuń") }
                }
            }
        }

        Section("Podpis klienta na ekranie") {
            SignaturePad(
                saved = e.signatureData,
                onSave = { vm.saveEstimate(e.copy(signatureData = it)) }
            )
        }
    }
}

@Composable
private fun SignaturePad(saved: String, onSave: (String) -> Unit) {
    var points by remember(saved) {
        mutableStateOf(
            saved.split("|").mapNotNull {
                val p = it.split(",")
                if (p.size == 2) {
                    val x = p[0].toFloatOrNull()
                    val y = p[1].toFloatOrNull()
                    if (x != null && y != null) Offset(x, y) else null
                } else null
            }
        )
    }

    Surface(Modifier.fillMaxWidth().height(160.dp), color = Color.White) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos -> points = points + pos },
                    onDrag = { change, _ ->
                        points = points + change.position
                        change.consume()
                    }
                )
            }
        ) {
            if (points.size > 1) {
                for (i in 1 until points.size) {
                    drawLine(Color.Black, points[i - 1], points[i], strokeWidth = 3f)
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = {
            onSave(points.joinToString("|") { "${it.x},${it.y}" })
        }) { Text("Zapisz podpis") }
        OutlinedButton(onClick = { points = emptyList(); onSave("") }) { Text("Wyczyść") }
    }
}

@Composable
private fun <T> rememberSaveableCompat(initializer: () -> MutableState<T>): MutableState<T> {
    return remember { initializer() }
}
