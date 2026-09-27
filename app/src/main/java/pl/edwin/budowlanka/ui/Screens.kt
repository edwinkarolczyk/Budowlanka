package pl.edwin.budowlanka.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pl.edwin.budowlanka.MainViewModel
import pl.edwin.budowlanka.data.*
import pl.edwin.budowlanka.util.BackupManager
import pl.edwin.budowlanka.util.UpdateChecker
import pl.edwin.budowlanka.util.UpdateInfo

@Composable
fun DashboardScreen(vm: MainViewModel, onOpenEstimate: (Long) -> Unit) {
    val estimates by vm.estimates.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val active = estimates.filter { it.status == EstimateStatus.ACCEPTED || it.status == EstimateStatus.IN_PROGRESS }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pulpit", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Wyceny")
                    Text(estimates.size.toString(), style = MaterialTheme.typography.headlineMedium)
                }
            }
            Card(Modifier.weight(1f)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Aktywne roboty")
                    Text(active.size.toString(), style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
        Section("Cel finansowy") {
            Text("Miesięcznie: ${money(settings?.monthlyTarget ?: 0.0)}")
            val days = settings?.workDaysMonth ?: 0
            val daily = if (days > 0) (settings?.monthlyTarget ?: 0.0) / days else 0.0
            Text("Wymagany średni dochód dzienny: ${money(daily)}")
        }
        Section("W realizacji / zaakceptowane") {
            if (active.isEmpty()) Text("Brak aktywnych zleceń.")
            active.forEach { e ->
                OutlinedButton(onClick = { onOpenEstimate(e.id) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(e.title)
                        Text("${e.status} • ${e.startDate.ifBlank { "termin nieustalony" }}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun EstimatesScreen(vm: MainViewModel, onOpenEstimate: (Long) -> Unit) {
    val estimates by vm.estimates.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Wyceny", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { showAdd = true }) { Text("+ Nowa") }
        }

        if (estimates.isEmpty()) {
            Text("Nie ma jeszcze wycen.")
        }
        estimates.forEach { e ->
            Card(onClick = { onOpenEstimate(e.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(e.title, style = MaterialTheme.typography.titleMedium)
                    Text(e.status)
                    if (e.startDate.isNotBlank()) Text("${e.startDate} – ${e.endDate.ifBlank { "?" }}")
                    Text(if (e.includeMaterials) "Robocizna + materiały" else "Tylko robocizna", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Nowa wycena") },
            text = {
                OutlinedTextField(title, { title = it }, label = { Text("Nazwa / opis") })
            },
            confirmButton = {
                Button(onClick = {
                    showAdd = false
                    vm.addEstimate(title) { onOpenEstimate(it) }
                }) { Text("Utwórz") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Anuluj") } }
        )
    }
}

@Composable
fun CatalogScreen(vm: MainViewModel) {
    val works by vm.works.collectAsStateWithLifecycle()
    val materials by vm.materials.collectAsStateWithLifecycle()
    val packages by vm.packages.collectAsStateWithLifecycle()
    val packageWorks by vm.packageWorks.collectAsStateWithLifecycle()
    val workMaterials by vm.workMaterials.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("Roboty") }
    var addWork by remember { mutableStateOf(false) }
    var addMaterial by remember { mutableStateOf(false) }
    var addPackage by remember { mutableStateOf(false) }
    var linkWorkId by remember { mutableStateOf<Long?>(null) }
    var packageId by remember { mutableStateOf<Long?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Baza", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Roboty", "Materiały", "Pakiety").forEach {
                if (tab == it) Button(onClick = { tab = it }) { Text(it) }
                else OutlinedButton(onClick = { tab = it }) { Text(it) }
            }
        }

        when (tab) {
            "Roboty" -> {
                Button(onClick = { addWork = true }) { Text("+ Dodaj robotę") }
                works.forEach { w ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(w.name, style = MaterialTheme.typography.titleMedium)
                            Text("${w.category} • ${w.unit} • ${money(w.laborRate)}/${w.unit}")
                            Text("Norma czasu: ${w.laborHoursPerUnit} h/${w.unit} • zapas ${w.defaultWastePct}%")
                            val links = workMaterials.filter { it.workId == w.id }
                            if (links.isNotEmpty()) {
                                Text("Materiały:", style = MaterialTheme.typography.labelMedium)
                                links.forEach { l ->
                                    val m = materials.firstOrNull { it.id == l.materialId }
                                    if (m != null) Text("• ${m.name}: ${l.qtyPerWorkUnit} ${m.unit}/${w.unit}")
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { linkWorkId = w.id }) { Text("+ materiał") }
                                TextButton(onClick = { vm.deleteWork(w) }) { Text("Usuń") }
                            }
                        }
                    }
                }
            }
            "Materiały" -> {
                Button(onClick = { addMaterial = true }) { Text("+ Dodaj materiał") }
                materials.forEach { m ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(m.name, style = MaterialTheme.typography.titleMedium)
                            if (m.manufacturer.isNotBlank()) Text(m.manufacturer)
                            Text("Tani ${money(m.priceBudget)} • Standard ${money(m.priceStandard)} • Premium ${money(m.pricePremium)} / ${m.unit}")
                            Text("Stan: ${m.stockQty} ${m.unit}", style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { vm.deleteMaterial(m) }) { Text("Usuń") }
                        }
                    }
                }
            }
            else -> {
                Button(onClick = { addPackage = true }) { Text("+ Dodaj pakiet") }
                packages.forEach { p ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(p.name, style = MaterialTheme.typography.titleMedium)
                            if (p.description.isNotBlank()) Text(p.description)
                            packageWorks.filter { it.packageId == p.id }.sortedBy { it.position }.forEach { pw ->
                                Text("• ${works.firstOrNull { it.id == pw.workId }?.name ?: "?"}")
                            }
                            OutlinedButton(onClick = { packageId = p.id }) { Text("+ robota do pakietu") }
                        }
                    }
                }
            }
        }
    }

    if (addWork) {
        WorkDialog(onDismiss = { addWork = false }) {
            vm.addWork(it); addWork = false
        }
    }
    if (addMaterial) {
        MaterialDialog(onDismiss = { addMaterial = false }) {
            vm.addMaterial(it); addMaterial = false
        }
    }
    if (addPackage) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addPackage = false },
            title = { Text("Nowy pakiet") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nazwa") })
                    OutlinedTextField(desc, { desc = it }, label = { Text("Opis") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addPackage(PackageEntity(name = name.ifBlank { "Nowy pakiet" }, description = desc))
                    addPackage = false
                }) { Text("Dodaj") }
            },
            dismissButton = { TextButton(onClick = { addPackage = false }) { Text("Anuluj") } }
        )
    }

    linkWorkId?.let { wid ->
        var selected by remember { mutableStateOf(materials.firstOrNull()?.id ?: 0L) }
        var qty by remember { mutableStateOf(1.0) }
        AlertDialog(
            onDismissRequest = { linkWorkId = null },
            title = { Text("Materiał do roboty") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimpleDropdown("Materiał", selected.toString(), materials.map { it.id.toString() to it.name }) {
                        selected = it.toLongOrNull() ?: 0L
                    }
                    NumberField("Zużycie na jednostkę roboty", qty, { qty = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (selected != 0L) vm.linkMaterial(wid, selected, qty)
                    linkWorkId = null
                }) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { linkWorkId = null }) { Text("Anuluj") } }
        )
    }

    packageId?.let { pid ->
        var selected by remember { mutableStateOf(works.firstOrNull()?.id ?: 0L) }
        AlertDialog(
            onDismissRequest = { packageId = null },
            title = { Text("Dodaj robotę do pakietu") },
            text = {
                SimpleDropdown("Robota", selected.toString(), works.map { it.id.toString() to it.name }) {
                    selected = it.toLongOrNull() ?: 0L
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pos = packageWorks.count { it.packageId == pid }
                    if (selected != 0L) vm.addWorkToPackage(pid, selected, pos)
                    packageId = null
                }) { Text("Dodaj") }
            },
            dismissButton = { TextButton(onClick = { packageId = null }) { Text("Anuluj") } }
        )
    }
}

@Composable
private fun WorkDialog(onDismiss: () -> Unit, onSave: (WorkEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Ogólne") }
    var unit by remember { mutableStateOf("m²") }
    var rate by remember { mutableStateOf(0.0) }
    var hours by remember { mutableStateOf(0.0) }
    var waste by remember { mutableStateOf(10.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa robota") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nazwa") })
                OutlinedTextField(category, { category = it }, label = { Text("Kategoria") })
                OutlinedTextField(unit, { unit = it }, label = { Text("Jednostka") })
                NumberField("Robocizna za jednostkę", rate, { rate = it })
                NumberField("Roboczogodziny / jednostkę", hours, { hours = it })
                NumberField("Domyślny zapas %", waste, { waste = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(WorkEntity(name = name.ifBlank { "Nowa robota" }, category = category, unit = unit, laborRate = rate, laborHoursPerUnit = hours, defaultWastePct = waste))
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun MaterialDialog(onDismiss: () -> Unit, onSave: (MaterialEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("szt.") }
    var manufacturer by remember { mutableStateOf("") }
    var cheap by remember { mutableStateOf(0.0) }
    var standard by remember { mutableStateOf(0.0) }
    var premium by remember { mutableStateOf(0.0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowy materiał") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nazwa") })
                OutlinedTextField(manufacturer, { manufacturer = it }, label = { Text("Producent / wariant") })
                OutlinedTextField(unit, { unit = it }, label = { Text("Jednostka") })
                NumberField("Cena tani", cheap, { cheap = it })
                NumberField("Cena standard", standard, { standard = it })
                NumberField("Cena premium", premium, { premium = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(MaterialEntity(name = name.ifBlank { "Nowy materiał" }, unit = unit, manufacturer = manufacturer, priceBudget = cheap, priceStandard = standard, pricePremium = premium))
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
fun ClientsScreen(vm: MainViewModel) {
    val clients by vm.clients.collectAsStateWithLifecycle()
    val sites by vm.sites.collectAsStateWithLifecycle()
    var addClient by remember { mutableStateOf(false) }
    var siteFor by remember { mutableStateOf<Long?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Klienci", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { addClient = true }) { Text("+ Klient") }
        }
        clients.forEach { c ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(c.name, style = MaterialTheme.typography.titleMedium)
                    if (c.phone.isNotBlank()) Text(c.phone)
                    if (c.email.isNotBlank()) Text(c.email)
                    Text("Stały rabat klienta: ${c.loyaltyDiscountPct}%")
                    sites.filter { it.clientId == c.id }.forEach { s ->
                        Text("• ${s.name.ifBlank { "Inwestycja" }} — ${s.address}")
                    }
                    OutlinedButton(onClick = { siteFor = c.id }) { Text("+ Adres / inwestycja") }
                }
            }
        }
    }

    if (addClient) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var discount by remember { mutableStateOf(0.0) }
        AlertDialog(
            onDismissRequest = { addClient = false },
            title = { Text("Nowy klient") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nazwa / imię") })
                    OutlinedTextField(phone, { phone = it }, label = { Text("Telefon") })
                    OutlinedTextField(email, { email = it }, label = { Text("E-mail") })
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notatki") })
                    NumberField("Własny rabat stały %", discount, { discount = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addClient(ClientEntity(name = name.ifBlank { "Klient" }, phone = phone, email = email, notes = notes, loyaltyDiscountPct = discount))
                    addClient = false
                }) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { addClient = false }) { Text("Anuluj") } }
        )
    }

    siteFor?.let { clientId ->
        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { siteFor = null },
            title = { Text("Nowa inwestycja") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nazwa") })
                    OutlinedTextField(address, { address = it }, label = { Text("Adres") })
                    OutlinedTextField(notes, { notes = it }, label = { Text("Notatki") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addSite(SiteEntity(clientId = clientId, name = name, address = address, notes = notes))
                    siteFor = null
                }) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { siteFor = null }) { Text("Anuluj") } }
        )
    }
}

@Composable
fun ToolsScreen(vm: MainViewModel) {
    val tools by vm.tools.collectAsStateWithLifecycle()
    val works by vm.works.collectAsStateWithLifecycle()
    val links by vm.workTools.collectAsStateWithLifecycle()
    var add by remember { mutableStateOf(false) }
    var linkToolId by remember { mutableStateOf<Long?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Narzędzia", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { add = true }) { Text("+ Narzędzie") }
        }
        tools.forEach { t ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(t.name, style = MaterialTheme.typography.titleMedium)
                    if (t.code.isNotBlank()) Text("Kod: ${t.code}")
                    links.filter { it.toolId == t.id }.forEach { l ->
                        Text("• ${works.firstOrNull { it.id == l.workId }?.name ?: "?"} × ${l.qty}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { linkToolId = t.id }) { Text("Przypisz do roboty") }
                        TextButton(onClick = { vm.deleteTool(t) }) { Text("Usuń") }
                    }
                }
            }
        }
    }

    if (add) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { add = false },
            title = { Text("Nowe narzędzie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nazwa") })
                    OutlinedTextField(code, { code = it }, label = { Text("Kod / oznaczenie") })
                    OutlinedTextField(notes, { notes = it }, label = { Text("Uwagi") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addTool(ToolEntity(name = name.ifBlank { "Narzędzie" }, code = code, notes = notes))
                    add = false
                }) { Text("Dodaj") }
            },
            dismissButton = { TextButton(onClick = { add = false }) { Text("Anuluj") } }
        )
    }

    linkToolId?.let { toolId ->
        var workId by remember { mutableStateOf(works.firstOrNull()?.id ?: 0L) }
        var qty by remember { mutableStateOf(1) }
        AlertDialog(
            onDismissRequest = { linkToolId = null },
            title = { Text("Przypisz narzędzie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SimpleDropdown("Robota", workId.toString(), works.map { it.id.toString() to it.name }) {
                        workId = it.toLongOrNull() ?: 0L
                    }
                    IntField("Ilość", qty, { qty = it.coerceAtLeast(1) })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (workId != 0L) vm.linkTool(workId, toolId, qty)
                    linkToolId = null
                }) { Text("Zapisz") }
            },
            dismissButton = { TextButton(onClick = { linkToolId = null }) { Text("Anuluj") } }
        )
    }
}

private fun datesOverlap(aStart: String, aEnd: String, bStart: String, bEnd: String): Boolean {
    if (aStart.isBlank() || bStart.isBlank()) return false
    val ae = aEnd.ifBlank { aStart }
    val be = bEnd.ifBlank { bStart }
    return aStart <= be && bStart <= ae
}

@Composable
fun CalendarScreen(vm: MainViewModel, onOpenEstimate: (Long) -> Unit) {
    val estimates by vm.estimates.collectAsStateWithLifecycle()
    val toolChecklist by vm.toolChecklist.collectAsStateWithLifecycle()
    val tools by vm.tools.collectAsStateWithLifecycle()
    val scheduled = estimates.filter { it.startDate.isNotBlank() }.sortedBy { it.startDate }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Kalendarz realizacji", style = MaterialTheme.typography.headlineSmall)
        Text("Daty wpisuj w formacie RRRR-MM-DD. Aplikacja wykrywa nakładanie terminów i narzędzi.", style = MaterialTheme.typography.bodySmall)

        scheduled.forEach { e ->
            val ownTools = toolChecklist.filter { it.estimateId == e.id }.map { it.toolId }.toSet()
            val conflicts = scheduled.filter { other ->
                other.id != e.id &&
                    other.status != EstimateStatus.REJECTED &&
                    datesOverlap(e.startDate, e.endDate, other.startDate, other.endDate)
            }
            val toolConflicts = conflicts.flatMap { other ->
                toolChecklist.filter { it.estimateId == other.id && it.toolId in ownTools }.map { it.toolId to other.title }
            }.distinct()

            Card(onClick = { onOpenEstimate(e.id) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(e.title, style = MaterialTheme.typography.titleMedium)
                    Text("${e.startDate} – ${e.endDate.ifBlank { e.startDate }} • ${e.status}")
                    if (conflicts.isNotEmpty()) {
                        Text("⚠ Kolizja terminu z: ${conflicts.joinToString { it.title }}", color = MaterialTheme.colorScheme.error)
                    }
                    toolConflicts.forEach { (toolId, title) ->
                        val name = tools.firstOrNull { it.id == toolId }?.name ?: "narzędzie"
                        Text("⚠ $name potrzebne też: $title", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (scheduled.isEmpty()) Text("Brak zleceń z terminem.")
    }
}

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settingsValue by vm.settings.collectAsStateWithLifecycle()
    val crew by vm.crewMembers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var addCrew by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var checked by remember { mutableStateOf(false) }

    val s = settingsValue ?: AppSettingsEntity()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ustawienia", style = MaterialTheme.typography.headlineSmall)

        Section("Dane firmy — trafią na ofertę PDF") {
            OutlinedTextField(s.companyName, { vm.saveSettings(s.copy(companyName = it)) }, label = { Text("Nazwa firmy") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.nip, { vm.saveSettings(s.copy(nip = it)) }, label = { Text("NIP") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.address, { vm.saveSettings(s.copy(address = it)) }, label = { Text("Adres") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.phone, { vm.saveSettings(s.copy(phone = it)) }, label = { Text("Telefon") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(s.email, { vm.saveSettings(s.copy(email = it)) }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
        }

        Section("Cel i domyślne koszty") {
            NumberField("Docelowy dochód miesięczny", s.monthlyTarget, { vm.saveSettings(s.copy(monthlyTarget = it)) }, Modifier.fillMaxWidth())
            IntField("Dni robocze w miesiącu", s.workDaysMonth, { vm.saveSettings(s.copy(workDaysMonth = it.coerceAtLeast(1))) }, Modifier.fillMaxWidth())
            NumberField("Godziny pracy / dzień", s.hoursPerDay, { vm.saveSettings(s.copy(hoursPerDay = it.coerceAtLeast(1.0))) }, Modifier.fillMaxWidth())
            NumberField("Stawka za km", s.defaultKmRate, { vm.saveSettings(s.copy(defaultKmRate = it)) }, Modifier.fillMaxWidth())
            NumberField("Domyślna stała opłata dojazdu", s.defaultFixedTravelFee, { vm.saveSettings(s.copy(defaultFixedTravelFee = it)) }, Modifier.fillMaxWidth())
        }

        Section("Ekipa — maks. 6 osób na zleceniu") {
            crew.forEach { c ->
                Text("• ${c.name} — ${c.defaultPayMode}, ${c.defaultRate}")
            }
            Button(onClick = { addCrew = true }) { Text("+ Osoba") }
        }

        Section("Kopia danych") {
            Text("ZIP zawiera dane aplikacji w JSON oraz dostępne zdjęcia.")
            Button(onClick = {
                scope.launch {
                    runCatching { BackupManager.shareBackup(context, vm.dao) }
                        .onFailure { Toast.makeText(context, "Błąd kopii: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            }) { Text("Eksportuj kopię ZIP") }
        }

        Section("Aktualizacje — jeden kanał") {
            Text("Wersja: 0.5.0")
            Button(onClick = {
                scope.launch {
                    update = UpdateChecker.check(context)
                    checked = true
                }
            }) { Text("Sprawdź aktualizację") }
            if (checked && update == null) Text("Brak nowszej wersji lub brak internetu.")
            update?.let { u ->
                Text("Dostępna: ${u.versionName}", style = MaterialTheme.typography.titleMedium)
                Text(u.changelog)
                Button(onClick = { UpdateChecker.download(context, u) }) { Text("Pobierz APK") }
            }
        }
    }

    if (addCrew) {
        var name by remember { mutableStateOf("") }
        var mode by remember { mutableStateOf(PayMode.PROFIT_PERCENT) }
        var rate by remember { mutableStateOf(0.0) }
        AlertDialog(
            onDismissRequest = { addCrew = false },
            title = { Text("Osoba w ekipie") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nazwa / imię") })
                    SimpleDropdown("Domyślne rozliczenie", mode, PayMode.all.map { it to it }) { mode = it }
                    NumberField(if (mode == PayMode.PROFIT_PERCENT) "Domyślny %" else "Stawka", rate, { rate = it })
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.addCrewMember(CrewMemberEntity(name = name.ifBlank { "Osoba" }, defaultPayMode = mode, defaultRate = rate))
                    addCrew = false
                }) { Text("Dodaj") }
            },
            dismissButton = { TextButton(onClick = { addCrew = false }) { Text("Anuluj") } }
        )
    }
}
