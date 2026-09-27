package pl.edwin.budowlanka.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.edwin.budowlanka.MainViewModel
import pl.edwin.budowlanka.data.*
import pl.edwin.budowlanka.domain.EstimateCalculator
import pl.edwin.budowlanka.domain.EstimateResult
import pl.edwin.budowlanka.domain.SchedulePlanner
import pl.edwin.budowlanka.util.PdfExporter
import pl.edwin.budowlanka.util.PdfPayload
import pl.edwin.budowlanka.util.RouteCalculator
import kotlinx.coroutines.launch
import kotlin.math.ceil

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
    val allOpenings by vm.openings.collectAsStateWithLifecycle()
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
        Box(Modifier.fillMaxSize().background(BudBg).padding(20.dp)) {
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

    var tab by rememberSaveableCompat { mutableStateOf("Klient") }
    val steps = listOf("Klient", "Wymiary", "Roboty", "Plan", "Wycena")

    Column(Modifier.fillMaxSize().background(BudBg)) {
        Surface(color = Color(0xFF090C0E), shadowElevation = 8.dp) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium, color = BudText) }
                    Column(Modifier.weight(1f)) {
                        Text(estimate.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${estimate.status} • ${money(result.clientTotal)}", color = BudMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Rounded.MoreVert, "Więcej", tint = BudText)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    steps.forEachIndexed { index, label ->
                        val selected = tab == label
                        Column(
                            modifier = Modifier.weight(1f).clickable { tab = label },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (selected) BudOrangeStrong else Color(0xFF4A5056),
                                modifier = Modifier.size(if (selected) 34.dp else 28.dp)
                                    .then(
                                        if (selected) Modifier.border(2.dp, BudOrangeLight, CircleShape)
                                        else Modifier
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        (index + 1).toString(),
                                        color = if (selected) Color.Black else BudText,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                color = if (selected) BudOrangeLight else BudMuted,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(Modifier.height(3.dp))
                            Box(
                                Modifier
                                    .height(3.dp)
                                    .width(if (selected) 34.dp else 0.dp)
                                    .background(if (selected) BudOrangeStrong else Color.Transparent, RoundedCornerShape(99.dp))
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (tab) {
                "Klient" -> ClientStep(vm, estimate, clients, sites)
                "Wymiary" -> MeasurementsTab(
                    vm,
                    estimate,
                    spaces,
                    allOpenings.filter { opening -> spaces.any { it.id == opening.spaceId } }
                )
                "Roboty" -> WorksTab(vm, estimate, works, materials, packages, packageWorks, spaces, lines, result)
                "Plan" -> PlanStep(vm, estimate, result, sites, estimates, crewMembers, crew, settings)
                else -> ValuationStep(
                    vm, estimate, result, clients, sites, estimates, works, allLines,
                    materials, workMaterials, tools, workTools, allSpaces, allCrew, allExtras,
                    crewMembers, crew, extras,
                    photos.filter { it.estimateId == estimateId },
                    shopping.filter { it.estimateId == estimateId },
                    toolChecklist.filter { it.estimateId == estimateId },
                    settings
                )
            }
        }
    }
}

@Composable
private fun ClientStep(
    vm: MainViewModel,
    e: EstimateEntity,
    clients: List<ClientEntity>,
    sites: List<SiteEntity>
) {
    var showQuickClient by remember { mutableStateOf(false) }
    val client = clients.firstOrNull { it.id == e.clientId }
    val clientSites = sites.filter { it.clientId == e.clientId }
    val selectedSite = clientSites.firstOrNull { it.id == e.siteId }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("1. Klient i adres", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Najpierw wybierz klienta i miejsce realizacji. Adres jest zawsze przypisany do klienta.", color = BudMuted)

        Section("Zlecenie") {
            OutlinedTextField(
                e.title,
                { vm.saveEstimate(e.copy(title = it)) },
                label = { Text("Nazwa wyceny / zlecenia") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Section("Klient") {
            SimpleDropdown(
                "Klient",
                (e.clientId ?: 0L).toString(),
                listOf("0" to "Wybierz klienta") + clients.map { it.id.toString() to it.name }
            ) { raw ->
                val id = raw.toLongOrNull()?.takeIf { it != 0L }
                val matchingSites = sites.filter { it.clientId == id }
                val defaultSite = matchingSites.singleOrNull()?.id
                vm.saveEstimate(e.copy(clientId = id, siteId = defaultSite))
            }

            Button(onClick = { showQuickClient = true }, modifier = Modifier.fillMaxWidth()) {
                Text("+ Nowy klient razem z adresem")
            }

            if (client != null) {
                if (client.phone.isNotBlank()) Text("Telefon: ${client.phone}")
                if (client.email.isNotBlank()) Text("E-mail: ${client.email}")
            }
        }

        if (e.clientId != null) {
            Section("Adres realizacji") {
                SimpleDropdown(
                    "Adres / inwestycja",
                    (e.siteId ?: 0L).toString(),
                    listOf("0" to "Wybierz adres") +
                        clientSites.map { it.id.toString() to "${it.name.ifBlank { "Inwestycja" }} • ${it.address}" }
                ) {
                    vm.saveEstimate(e.copy(siteId = it.toLongOrNull()?.takeIf { id -> id != 0L }))
                }

                if (selectedSite != null) {
                    Text(selectedSite.address, color = BudSelectedStrong, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Section("Uwagi z oględzin") {
            OutlinedTextField(
                e.notes,
                { vm.saveEstimate(e.copy(notes = it)) },
                label = { Text("Uwagi") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }

    if (showQuickClient) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var siteName by remember { mutableStateOf("Adres główny") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickClient = false },
            title = { Text("Nowy klient + adres") },
            text = {
                Column(
                    Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nazwa / imię") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(siteName, { siteName = it }, label = { Text("Nazwa miejsca") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(address, { address = it }, label = { Text("Pełny adres realizacji") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notatki") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addClientWithSite(
                        ClientEntity(
                            name = name.ifBlank { "Klient" },
                            phone = phone,
                            email = email,
                            notes = notes
                        ),
                        siteName,
                        address
                    ) { clientId, siteId ->
                        vm.saveEstimate(e.copy(clientId = clientId, siteId = siteId))
                    }
                    showQuickClient = false
                }) { Text("Dodaj i wybierz") }
            },
            dismissButton = { TextButton(onClick = { showQuickClient = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun PlanStep(
    vm: MainViewModel,
    e: EstimateEntity,
    result: EstimateResult,
    sites: List<SiteEntity>,
    allEstimates: List<EstimateEntity>,
    crewMembers: List<CrewMemberEntity>,
    crew: List<EstimateCrewEntity>,
    settings: AppSettingsEntity?
) {
    val scope = rememberCoroutineScope()
    val selectedSite = sites.firstOrNull { it.id == e.siteId }
    val origin = settings?.address.orEmpty()
    val destination = selectedSite?.address.orEmpty()

    var routeLoading by remember { mutableStateOf(false) }
    var routeMessage by remember { mutableStateOf("") }

    val proposed = remember(e.id, result.technicalDays, allEstimates) {
        SchedulePlanner.propose(e.id, result.technicalDays, allEstimates)
    }

    suspend fun recalcRoute() {
        if (origin.isBlank() || destination.isBlank()) {
            routeMessage = if (origin.isBlank()) {
                "Uzupełnij adres firmy w Ustawieniach."
            } else {
                "Wybierz adres klienta."
            }
            return
        }
        routeLoading = true
        RouteCalculator.calculate(origin, destination)
            .onSuccess { route ->
                val travelDays = e.travelDays.takeIf { it > 0 }
                    ?: ceil(result.technicalDays.coerceAtLeast(1.0)).toInt()
                vm.saveEstimate(
                    e.copy(
                        oneWayKm = route.oneWayKm,
                        travelDays = travelDays
                    )
                )
                routeMessage = "Trasa: ${"%.1f".format(route.oneWayKm)} km w jedną stronę."
            }
            .onFailure { routeMessage = it.message ?: "Nie udało się policzyć trasy." }
        routeLoading = false
    }

    LaunchedEffect(e.siteId, origin, destination) {
        if (origin.isNotBlank() && destination.isNotBlank()) recalcRoute()
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("4. Plan realizacji", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Section("Proponowany termin") {
            Text(
                "${proposed.first} → ${proposed.second}",
                color = BudSelectedStrong,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text("Na podstawie czasu technicznego i już zajętych terminów.", color = BudMuted)
            Button(
                onClick = {
                    vm.saveEstimate(e.copy(startDate = proposed.first, endDate = proposed.second))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Dodaj proponowany termin") }

            if (e.startDate.isNotBlank()) {
                Text("Ustalony termin: ${e.startDate} → ${e.endDate.ifBlank { e.startDate }}")
            }
        }

        Section("Dojazd — automatycznie z adresów") {
            Text("Start: ${origin.ifBlank { "brak adresu firmy" }}", color = BudMuted)
            Text("Cel: ${destination.ifBlank { "brak adresu klienta" }}", color = BudMuted)
            if (routeLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (routeMessage.isNotBlank()) Text(routeMessage)
            if (e.oneWayKm > 0.0) {
                Text("Odległość: ${"%.1f".format(e.oneWayKm)} km w jedną stronę", color = BudSelectedStrong, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { scope.launch { recalcRoute() } },
                enabled = !routeLoading,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Przelicz trasę ponownie") }

            IntField("Liczba dni przejazdu", e.travelDays, { vm.saveEstimate(e.copy(travelDays = it)) }, Modifier.fillMaxWidth())
            NumberField("Stawka za km", e.kmRate, { vm.saveEstimate(e.copy(kmRate = it)) }, Modifier.fillMaxWidth())
            NumberField("Stała kwota dojazdu", e.fixedTravelFee, { vm.saveEstimate(e.copy(fixedTravelFee = it)) }, Modifier.fillMaxWidth())
            Text("Dojazd A↔B × dni + stała opłata = ${money(result.travelCost)}")
        }

        Section("Czas techniczny") {
            Text("Roboczogodziny: ${"%.1f".format(result.laborHours)} h")
            Text("Szacowany czas: ${"%.1f".format(result.technicalDays)} dni")
        }

        Text("Ekipa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        CrewTab(vm, e, crewMembers, crew, result)
    }
}

@Composable
private fun ValuationStep(
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
    allExtras: List<ExtraCostEntity>,
    crewMembers: List<CrewMemberEntity>,
    crew: List<EstimateCrewEntity>,
    extras: List<ExtraCostEntity>,
    photos: List<PhotoEntity>,
    shopping: List<ShoppingItemEntity>,
    checklist: List<ToolChecklistEntity>,
    settings: AppSettingsEntity?
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
    var showRealization by remember { mutableStateOf(false) }

    if (showRealization) {
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = { showRealization = false }) { Text("‹ Wróć do wyceny") }
            RealizationTab(
                vm, e, result, works, allLines.filter { it.estimateId == e.id }, materials, clients, sites,
                crewMembers, crew, extras, spaces, photos, shopping, checklist, tools, settings
            )
        }
        return
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("5. Wycena końcowa", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Dopiero tutaj ustawiasz sposób wyceny, marże, rabat i finalną cenę dla klienta.", color = BudMuted)

        Section("Sposób wyceny") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(e.includeMaterials, { vm.saveEstimate(e.copy(includeMaterials = it)) })
                Text(if (e.includeMaterials) "Robocizna + materiały" else "Tylko robocizna")
            }
            NumberField("Marża robocizny %", e.marginLaborPct, { vm.saveEstimate(e.copy(marginLaborPct = it)) }, Modifier.fillMaxWidth())
            NumberField("Marża materiałów %", e.marginMaterialPct, { vm.saveEstimate(e.copy(marginMaterialPct = it)) }, Modifier.fillMaxWidth())
            NumberField("Marża całego zlecenia %", e.marginOverallPct, { vm.saveEstimate(e.copy(marginOverallPct = it)) }, Modifier.fillMaxWidth())

            if (client != null) {
                Text("Historia klienta: ${money(completedSpend)}", color = BudMuted)
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

        Section("Koszty dodatkowe") {
            allExtras.filter { it.estimateId == e.id }.forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${it.category}: ${it.description}")
                    Text(money(it.amount))
                }
            }
            Button(onClick = { showExtra = true }) { Text("+ Koszt dodatkowy") }
        }

        Section("Końcowe podsumowanie") {
            ResultSummary(result)
            Text("Dojazd: ${money(result.travelCost)}")
            if (result.technicalDays > result.financialMaxDays && result.financialMaxDays > 0.0) {
                Text("⚠ Techniczny czas przekracza termin wynikający z celu dochodowego.", color = MaterialTheme.colorScheme.error)
            } else if (result.financialMaxDays > 0.0) {
                Text("✓ Zlecenie mieści się w założeniu dochodowym.", color = BudGreen)
            }
        }

        Section("Status i finalizacja") {
            SimpleDropdown("Status", e.status, EstimateStatus.all.map { it to it }) {
                vm.saveEstimate(e.copy(status = it))
            }
            if (e.startDate.isNotBlank()) Text("Termin: ${e.startDate} → ${e.endDate.ifBlank { e.startDate }}")
            Button(onClick = { showRealization = true }, modifier = Modifier.fillMaxWidth()) {
                Text("PDF / akceptacja / realizacja")
            }
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
                    OutlinedTextField(category, { category = it }, label = { Text("Kategoria") })
                    OutlinedTextField(desc, { desc = it }, label = { Text("Opis") })
                    NumberField("Kwota", amount, { amount = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addExtra(
                        ExtraCostEntity(
                            estimateId = e.id,
                            category = category,
                            description = desc.ifBlank { category },
                            amount = amount
                        )
                    )
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
private fun MeasurementsTab(
    vm: MainViewModel,
    e: EstimateEntity,
    spaces: List<SpaceEntity>,
    openings: List<OpeningEntity>
) {
    var showAdd by remember { mutableStateOf(false) }
    var edit by remember { mutableStateOf<SpaceEntity?>(null) }
    var openingSpaceId by remember { mutableStateOf<Long?>(null) }
    var editOpening by remember { mutableStateOf<OpeningEntity?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("2. Wymiary", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = { showAdd = true }) { Text("+ Pomieszczenie") }
        }

        Text(
            "Dodaj pomieszczenie, a potem każde okno i drzwi osobno. Aplikacja sama odejmie ich powierzchnię od ścian.",
            color = BudMuted
        )

        spaces.forEach { s ->
            val roomOpenings = openings.filter { it.spaceId == s.id }
            Card(colors = CardDefaults.cardColors(containerColor = BudPanel), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("${s.level} • ${s.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${s.length} × ${s.width} × h ${s.height} m", color = BudMuted)
                    Text("Podłoga/sufit: ${"%.2f".format(s.floorArea())} m²")
                    Text("Ściany po odjęciu otworów: ${"%.2f".format(s.wallArea())} m²")
                    Text("Otwory razem: ${"%.2f".format(s.openingsArea)} m²", color = BudSelectedStrong)

                    if (roomOpenings.isNotEmpty()) {
                        HorizontalDivider(color = BudLine)
                        roomOpenings.forEach { o ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${o.type}: ${o.name.ifBlank { o.type }}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${o.quantity} × ${o.width} × ${o.height} m = ${"%.2f".format(o.area())} m²",
                                        color = BudMuted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(onClick = { editOpening = o }) { Text("Edytuj") }
                                TextButton(onClick = { vm.deleteOpening(o) }) { Text("Usuń") }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { openingSpaceId = s.id },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Okno / drzwi")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { edit = s }) { Text("Edytuj pokój") }
                        OutlinedButton(onClick = { vm.duplicateSpace(s) }) { Text("Duplikuj") }
                        TextButton(onClick = { vm.deleteSpace(s) }) { Text("Usuń") }
                    }
                }
            }
        }
    }

    if (showAdd) {
        SpaceDialog(
            estimateId = e.id,
            current = null,
            onDismiss = { showAdd = false },
            onSave = {
                vm.addSpace(it)
                showAdd = false
            }
        )
    }

    edit?.let { current ->
        SpaceDialog(
            estimateId = e.id,
            current = current,
            onDismiss = { edit = null },
            onSave = {
                vm.addSpace(it)
                edit = null
            }
        )
    }

    openingSpaceId?.let { sid ->
        OpeningDialog(
            spaceId = sid,
            current = null,
            onDismiss = { openingSpaceId = null },
            onSave = {
                vm.addOpening(it)
                openingSpaceId = null
            }
        )
    }

    editOpening?.let { current ->
        OpeningDialog(
            spaceId = current.spaceId,
            current = current,
            onDismiss = { editOpening = null },
            onSave = {
                vm.addOpening(it)
                editOpening = null
            }
        )
    }
}

@Composable
private fun SpaceDialog(
    estimateId: Long,
    current: SpaceEntity?,
    onDismiss: () -> Unit,
    onSave: (SpaceEntity) -> Unit
) {
    var level by remember(current) { mutableStateOf(current?.level ?: "Parter") }
    var name by remember(current) { mutableStateOf(current?.name ?: "") }
    var l by remember(current) { mutableStateOf(current?.length ?: 0.0) }
    var w by remember(current) { mutableStateOf(current?.width ?: 0.0) }
    var h by remember(current) { mutableStateOf(current?.height ?: 2.6) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == null) "Pomieszczenie" else "Edytuj pomieszczenie") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(level, { level = it }, label = { Text("Poziom / strefa") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(name, { name = it }, label = { Text("Nazwa") }, modifier = Modifier.fillMaxWidth())
                NumberField("Długość [m]", l, { l = it }, Modifier.fillMaxWidth())
                NumberField("Szerokość [m]", w, { w = it }, Modifier.fillMaxWidth())
                NumberField("Wysokość [m]", h, { h = it }, Modifier.fillMaxWidth())
                Text("Okna i drzwi dodasz osobno po zapisaniu pomieszczenia.", color = BudMuted)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    SpaceEntity(
                        id = current?.id ?: 0L,
                        estimateId = estimateId,
                        level = level,
                        name = name.ifBlank { "Pomieszczenie" },
                        length = l,
                        width = w,
                        height = h,
                        openingsArea = current?.openingsArea ?: 0.0
                    )
                )
            }) { Text(if (current == null) "Dodaj" else "Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun OpeningDialog(
    spaceId: Long,
    current: OpeningEntity?,
    onDismiss: () -> Unit,
    onSave: (OpeningEntity) -> Unit
) {
    var type by remember(current) { mutableStateOf(current?.type ?: OpeningType.WINDOW) }
    var name by remember(current) { mutableStateOf(current?.name ?: "") }
    var width by remember(current) { mutableStateOf(current?.width ?: 0.0) }
    var height by remember(current) { mutableStateOf(current?.height ?: 0.0) }
    var quantity by remember(current) { mutableStateOf(current?.quantity ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == null) "Dodaj okno / drzwi" else "Edytuj otwór") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleDropdown("Typ", type, OpeningType.all.map { it to it }) { type = it }
                OutlinedTextField(
                    name,
                    { name = it },
                    label = { Text("Nazwa, np. okno balkonowe") },
                    modifier = Modifier.fillMaxWidth()
                )
                NumberField("Szerokość [m]", width, { width = it }, Modifier.fillMaxWidth())
                NumberField("Wysokość [m]", height, { height = it }, Modifier.fillMaxWidth())
                IntField("Ilość takich samych", quantity, { quantity = it.coerceAtLeast(1) }, Modifier.fillMaxWidth())
                Text(
                    "Powierzchnia: ${"%.2f".format(width * height * quantity.coerceAtLeast(1))} m²",
                    color = BudSelectedStrong,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    OpeningEntity(
                        id = current?.id ?: 0L,
                        spaceId = spaceId,
                        type = type,
                        name = name,
                        width = width,
                        height = height,
                        quantity = quantity.coerceAtLeast(1)
                    )
                )
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
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
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(lines.map { it.id }) {
        selectedIds = selectedIds.intersect(lines.map { it.id }.toSet())
    }

    val visibleLines = lines.filter { line ->
        val work = works.firstOrNull { it.id == line.workId }
        query.isBlank() || work?.name?.contains(query, ignoreCase = true) == true
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Żywy zakres robót", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Suma jest przeliczana po każdej zmianie.", color = BudMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text(money(result.clientTotal), color = BudOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = { add = true }) { Text("+ Robota") }
            if (selectedIds.isNotEmpty()) {
                OutlinedButton(onClick = {
                    val chosen = lines.filter { it.id in selectedIds }
                    vm.deleteEstimateWorks(chosen)
                    selectedIds = emptySet()
                }) {
                    Text("Usuń zaznaczone (${selectedIds.size})")
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Szukaj w zakresie") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (packages.isNotEmpty()) {
            Section("Pakiety") {
                packages.forEach { p ->
                    OutlinedButton(
                        onClick = { vm.addPackageToEstimate(e.id, p.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ ${p.name}")
                    }
                }
            }
        }

        visibleLines.forEach { line ->
            val work = works.firstOrNull { it.id == line.workId } ?: return@forEach
            val qty = EstimateCalculator.resolveQuantity(line, spaces)
            val unitRate = EstimateCalculator.resolveLaborRate(line, work)
            val laborValue = qty * unitRate
            val selected = line.id in selectedIds

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) BudSelectedSoft else BudPanel
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (selected) Modifier.border(2.dp, BudOrangeStrong, RoundedCornerShape(12.dp))
                        else Modifier
                    )
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + line.id else selectedIds - line.id
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = BudOrangeStrong,
                                checkmarkColor = Color.Black,
                                uncheckedColor = BudMuted
                            )
                        )
                        Column(Modifier.weight(1f)) {
                            Text(work.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val room = spaces.firstOrNull { it.id == line.spaceId }?.name
                            if (room != null) Text(room, color = BudMuted, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(
                            money(laborValue),
                            color = if (selected) BudOrangeLight else BudOrange,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "${"%.2f".format(qty)} ${work.unit} × ${money(unitRate)}/${work.unit}",
                        color = BudMuted
                    )
                    val priceModeLabel = when (line.laborPriceMode) {
                        LaborPriceMode.MIN -> "MIN z widełek"
                        LaborPriceMode.MAX -> "MAX z widełek"
                        LaborPriceMode.CUSTOM -> "Własna stawka"
                        else -> "Cena katalogowa"
                    }
                    Text(
                        priceModeLabel,
                        color = if (line.laborPriceMode == LaborPriceMode.CATALOG) BudMuted else BudOrangeLight,
                        fontWeight = if (line.laborPriceMode == LaborPriceMode.CATALOG) FontWeight.Normal else FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                    if (work.priceRegion.isNotBlank()) {
                        Text("${work.priceRegion} ${work.priceYear}", color = BudMuted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "Materiał: ${line.materialTier} • zapas ${line.wastePctOverride ?: work.defaultWastePct}%",
                        color = BudMuted,
                        style = MaterialTheme.typography.labelSmall
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { edit = line }) { Text("Edytuj") }
                        OutlinedButton(onClick = { vm.duplicateEstimateWork(line) }) { Text("Duplikuj") }
                        TextButton(onClick = { vm.deleteEstimateWork(line) }) { Text("Usuń") }
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
            vm.addEstimateWork(it)
            add = false
        }
    }

    edit?.let { current ->
        WorkLineDialog(e.id, current, works, spaces, onDismiss = { edit = null }) {
            vm.addEstimateWork(it)
            edit = null
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
    var workSearch by remember { mutableStateOf("") }
    var spaceId by remember { mutableStateOf(current?.spaceId ?: 0L) }
    var source by remember { mutableStateOf(current?.quantitySource ?: QuantitySource.MANUAL) }
    var qty by remember { mutableStateOf(current?.quantity ?: 1.0) }
    var tier by remember { mutableStateOf(current?.materialTier ?: MaterialTier.STANDARD) }
    var waste by remember { mutableStateOf(current?.wastePctOverride ?: -1.0) }
    var priceMode by remember {
        mutableStateOf(
            current?.laborPriceMode
                ?: if (current?.laborRateOverride != null) LaborPriceMode.CUSTOM else LaborPriceMode.CATALOG
        )
    }
    var customRate by remember {
        mutableStateOf(
            current?.laborRateOverride
                ?: works.firstOrNull { it.id == workId }?.laborRate
                ?: 0.0
        )
    }

    val selectedWork = works.firstOrNull { it.id == workId }

    LaunchedEffect(workId) {
        if (current == null && selectedWork != null) {
            source = EstimateCalculator.suggestedQuantitySource(selectedWork)
        }
    }

    val filteredWorks = works.filter {
        workSearch.isBlank() ||
            it.name.contains(workSearch, ignoreCase = true) ||
            it.category.contains(workSearch, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (current == null) "Dodaj robotę" else "Edytuj robotę") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = workSearch,
                    onValueChange = { workSearch = it },
                    label = { Text("Szukaj roboty") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SimpleDropdown(
                    "Robota",
                    workId.toString(),
                    filteredWorks.map { it.id.toString() to "${it.category} • ${it.name}" }
                ) {
                    workId = it.toLongOrNull() ?: 0L
                    if (priceMode != LaborPriceMode.CUSTOM) {
                        customRate = works.firstOrNull { w -> w.id == workId }?.laborRate ?: 0.0
                    }
                }

                if (selectedWork != null) {
                    Text(
                        "Cennik: ${money(selectedWork.laborRate)}/${selectedWork.unit}" +
                            if (selectedWork.priceRegion.isNotBlank()) " • ${selectedWork.priceRegion} ${selectedWork.priceYear}" else "",
                        color = BudMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                SimpleDropdown(
                    "Pomieszczenie",
                    spaceId.toString(),
                    listOf("0" to "Brak / całe zlecenie") +
                        spaces.map { it.id.toString() to "${it.level} • ${it.name}" }
                ) {
                    spaceId = it.toLongOrNull() ?: 0L
                }

                SimpleDropdown(
                    "Źródło obmiaru",
                    source,
                    QuantitySource.all.map { it to QuantitySource.label(it) }
                ) { source = it }

                if (source == QuantitySource.MANUAL || source == QuantitySource.PIECES) {
                    NumberField(
                        if (source == QuantitySource.PIECES) "Liczba sztuk" else "Ilość",
                        qty,
                        { qty = it },
                        Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        if (spaceId == 0L) "Obmiar zostanie zsumowany ze wszystkich pomieszczeń."
                        else "Obmiar zostanie pobrany z wybranego pomieszczenia.",
                        color = BudMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text("Cena robocizny", fontWeight = FontWeight.SemiBold)
                val priceModes = listOf(
                    LaborPriceMode.MIN to "MIN",
                    LaborPriceMode.CATALOG to "KATALOG",
                    LaborPriceMode.MAX to "MAX",
                    LaborPriceMode.CUSTOM to "WŁASNA"
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priceModes.forEach { (mode, label) ->
                        val active = priceMode == mode
                        Surface(
                            color = if (active) BudOrangeStrong else BudPanel2,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (active) Modifier.border(2.dp, BudOrangeLight, RoundedCornerShape(9.dp))
                                    else Modifier
                                )
                                .clickable {
                                    priceMode = mode
                                    if (mode == LaborPriceMode.CUSTOM && customRate == 0.0) {
                                        customRate = selectedWork?.laborRate ?: 0.0
                                    }
                                }
                        ) {
                            Text(
                                label,
                                color = if (active) Color.Black else BudMuted,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                if (selectedWork != null) {
                    Text(
                        "MIN ${money(selectedWork.laborRateLow.takeIf { it > 0.0 } ?: selectedWork.laborRate)} • " +
                            "KATALOG ${money(selectedWork.laborRate)} • " +
                            "MAX ${money(selectedWork.laborRateHigh.takeIf { it > 0.0 } ?: selectedWork.laborRate)}",
                        color = BudMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (priceMode == LaborPriceMode.CUSTOM) {
                    NumberField("Cena robocizny /${selectedWork?.unit ?: "j.m."}", customRate, { customRate = it }, Modifier.fillMaxWidth())
                }

                SimpleDropdown("Klasa materiału", tier, MaterialTier.all.map { it to it }) { tier = it }
                NumberField("Zapas % (-1 = domyślny roboty)", waste, { waste = it }, Modifier.fillMaxWidth())
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
                        wastePctOverride = waste.takeIf { it >= 0.0 },
                        laborRateOverride = customRate.takeIf { priceMode == LaborPriceMode.CUSTOM },
                        laborPriceMode = priceMode
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
        Modifier.fillMaxWidth(),
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
