package pl.edwin.budowlanka.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pl.edwin.budowlanka.MainViewModel
import pl.edwin.budowlanka.data.*
import pl.edwin.budowlanka.util.BackupManager
import pl.edwin.budowlanka.util.UpdateChecker
import pl.edwin.budowlanka.util.UpdateInfo

@Composable
fun DashboardScreen(
    vm: MainViewModel,
    onOpenEstimate: (Long) -> Unit,
    onNewEstimate: () -> Unit = {},
    onClients: () -> Unit = {},
    onCatalog: () -> Unit = {},
    onTools: () -> Unit = {},
    onCalendar: () -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val estimates by vm.estimates.collectAsStateWithLifecycle()
    val works by vm.works.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(BudBg).verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = BudPanel2),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.LocationOn, null, tint = BudOrange, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Małopolskie 2026", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Domyślne ceny i normy", color = BudMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Surface(shape = RoundedCornerShape(10.dp), color = BudOrange) {
                    Text("AKTYWNE", color = Color.Black, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
                }
            }
        }

        val tiles = listOf(
            Triple("Nowy kosztorys", Icons.Rounded.NoteAdd, onNewEstimate),
            Triple("Klienci", Icons.Rounded.Groups, onClients),
            Triple("Roboty", Icons.Rounded.Wallpaper, onCatalog),
            Triple("Materiały", Icons.Rounded.Inventory2, onCatalog),
            Triple("Pakiety robót", Icons.Rounded.Layers, onCatalog),
            Triple("Narzędzia", Icons.Rounded.Handyman, onTools),
            Triple("Realizacje", Icons.Rounded.AssignmentTurnedIn, { }),
            Triple("Kalendarz", Icons.Rounded.CalendarMonth, onCalendar),
            Triple("Ustawienia", Icons.Rounded.Settings, onSettings)
        )
        tiles.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, icon, action) ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BudPanel),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(92.dp).clickable { action() }
                    ) {
                        Column(
                            Modifier.fillMaxSize().padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(icon, null, tint = BudOrange, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.height(7.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2)
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ostatnie kosztorysy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${works.size} pozycji w cenniku", color = BudOrange, style = MaterialTheme.typography.labelMedium)
        }

        if (estimates.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = BudPanel), modifier = Modifier.fillMaxWidth()) {
                Text("Brak kosztorysów. Utwórz pierwszy.", color = BudMuted, modifier = Modifier.padding(16.dp))
            }
        } else {
            estimates.take(5).forEach { e ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BudPanel),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenEstimate(e.id) }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.ReceiptLong, null, tint = BudOrange)
                            Column {
                                Text(e.title, fontWeight = FontWeight.SemiBold)
                                Text(e.status, color = if (e.status == EstimateStatus.DONE) BudGreen else BudMuted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = BudMuted)
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
    val priceHistory by vm.priceHistory.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf("Roboty") }
    var filter by remember { mutableStateOf("Wszystkie") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var addWork by remember { mutableStateOf(false) }
    var addMaterial by remember { mutableStateOf(false) }
    var addPackage by remember { mutableStateOf(false) }
    var editWork by remember { mutableStateOf<WorkEntity?>(null) }
    var editMaterial by remember { mutableStateOf<MaterialEntity?>(null) }
    var linkWorkId by remember { mutableStateOf<Long?>(null) }
    var packageId by remember { mutableStateOf<Long?>(null) }

    val categoryIcons: Map<String, ImageVector> = mapOf(
        "Roboty ziemne" to Icons.Rounded.Agriculture,
        "Fundamenty" to Icons.Rounded.Foundation,
        "Ściany i murowanie" to Icons.Rounded.Wallpaper,
        "Stropy i żelbet" to Icons.Rounded.GridOn,
        "Dachy" to Icons.Rounded.Roofing,
        "Izolacje i ocieplenia" to Icons.Rounded.Layers,
        "Elewacje" to Icons.Rounded.House,
        "Tynki i gładzie" to Icons.Rounded.FormatPaint,
        "Malowanie" to Icons.Rounded.FormatPaint,
        "Płyty GK" to Icons.Rounded.ViewQuilt,
        "Posadzki i wylewki" to Icons.Rounded.Texture,
        "Płytki i okładziny" to Icons.Rounded.GridView,
        "Panele i podłogi" to Icons.Rounded.ViewWeek,
        "Instalacje elektryczne" to Icons.Rounded.ElectricBolt,
        "Instalacje sanitarne" to Icons.Rounded.Plumbing,
        "Stolarka" to Icons.Rounded.Window,
        "Kominy" to Icons.Rounded.Factory,
        "Rozbiórki i bruzdy" to Icons.Rounded.Construction,
        "Stawki r-g kosztorysowe" to Icons.Rounded.Calculate,
        "Stawki godzinowe — rynek" to Icons.Rounded.Schedule,
        "Ogrzewanie podłogowe" to Icons.Rounded.DeviceThermostat
    )

    Column(
        Modifier.fillMaxSize().background(BudBg).verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedCategory != null) {
                    IconButton(onClick = { selectedCategory = null }) { Icon(Icons.Rounded.ArrowBack, "Wróć") }
                }
                Text(selectedCategory ?: "Roboty", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Rounded.MoreVert, null, tint = BudMuted)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Wszystkie", "Ulubione", "Moje").forEach { item ->
                val active = filter == item
                Surface(
                    color = if (active) BudOrange else BudPanel2,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.weight(1f).clickable { filter = item }
                ) {
                    Text(
                        item,
                        color = if (active) Color.Black else BudText,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            label = { Text("Szukaj robót") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Roboty", "Materiały", "Pakiety").forEach {
                if (mode == it) Button(onClick = { mode = it }, shape = RoundedCornerShape(9.dp)) { Text(it) }
                else OutlinedButton(onClick = { mode = it }, shape = RoundedCornerShape(9.dp)) { Text(it) }
            }
        }

        when (mode) {
            "Roboty" -> {
                if (selectedCategory == null) {
                    val categories = works
                        .filter { query.isBlank() || it.name.contains(query, true) || it.category.contains(query, true) }
                        .groupBy { it.category }
                        .toSortedMap()
                    categories.forEach { (cat, list) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BudPanel),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().clickable { selectedCategory = cat }
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(categoryIcons[cat] ?: Icons.Rounded.Construction, null, tint = BudOrange, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cat, fontWeight = FontWeight.SemiBold)
                                    Text("${list.size} pozycji", color = BudMuted, style = MaterialTheme.typography.labelSmall)
                                }
                                Icon(Icons.Rounded.ChevronRight, null, tint = BudMuted)
                            }
                        }
                    }
                    Button(onClick = { addWork = true }, modifier = Modifier.fillMaxWidth()) { Text("+ Dodaj własną robotę") }
                } else {
                    val list = works.filter {
                        it.category == selectedCategory &&
                            (query.isBlank() || it.name.contains(query, true)) &&
                            filter != "Ulubione"
                    }
                    if (filter == "Ulubione") {
                        Text("Ulubione dodamy w kolejnym kroku. Ten filtr jest już przygotowany w wyglądzie.", color = BudMuted)
                    } else {
                        list.forEach { w ->
                            Card(colors = CardDefaults.cardColors(containerColor = BudPanel), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(w.name, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                        Text(money(w.laborRate), color = BudOrange, fontWeight = FontWeight.Bold)
                                    }
                                    val range = if (w.laborRateLow > 0 && w.laborRateHigh > w.laborRateLow)
                                        "Widełki: ${money(w.laborRateLow)}–${money(w.laborRateHigh)}/${w.unit}"
                                    else "Stawka: ${money(w.laborRate)}/${w.unit}"
                                    Text(range, color = BudMuted, style = MaterialTheme.typography.bodySmall)
                                    if (w.priceYear > 0) {
                                        Text("${w.priceRegion} • ${w.priceYear}${if (w.includesMaterial) " • z materiałem" else ""}", color = BudMuted, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (w.priceSource.isNotBlank()) Text(w.priceSource, color = BudMuted, style = MaterialTheme.typography.labelSmall)
                                    val links = workMaterials.filter { it.workId == w.id }
                                    if (links.isNotEmpty()) {
                                        Text("Materiały: " + links.joinToString { l ->
                                            val m = materials.firstOrNull { it.id == l.materialId }
                                            if (m != null) "${m.name} ${l.qtyPerWorkUnit} ${m.unit}/${w.unit}" else ""
                                        }, color = BudMuted, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { editWork = w }) { Text("Edytuj") }
                                        OutlinedButton(onClick = { linkWorkId = w.id }) { Text("+ materiał") }
                                        TextButton(onClick = { vm.deleteWork(w) }) { Text("Usuń") }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Materiały" -> {
                Button(onClick = { addMaterial = true }) { Text("+ Dodaj materiał") }
                materials.filter { query.isBlank() || it.name.contains(query, true) }.forEach { m ->
                    Card(colors = CardDefaults.cardColors(containerColor = BudPanel), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(m.name, fontWeight = FontWeight.SemiBold)
                            Text("Tani ${money(m.priceBudget)} • Standard ${money(m.priceStandard)} • Premium ${money(m.pricePremium)} / ${m.unit}", color = BudMuted)
                            Text("Stan: ${m.stockQty} ${m.unit}", color = BudMuted, style = MaterialTheme.typography.labelSmall)
                            val history = priceHistory.filter { it.materialId == m.id }.take(3)
                            if (history.isNotEmpty()) Text("Historia cen: ${history.size} ostatnie wpisy", color = BudMuted, style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = { editMaterial = m }) { Text("Edytuj") }
                                TextButton(onClick = { vm.deleteMaterial(m) }) { Text("Usuń") }
                            }
                        }
                    }
                }
            }

            else -> {
                Button(onClick = { addPackage = true }) { Text("+ Dodaj pakiet") }
                packages.forEach { p ->
                    Card(colors = CardDefaults.cardColors(containerColor = BudPanel), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(p.name, fontWeight = FontWeight.SemiBold)
                            if (p.description.isNotBlank()) Text(p.description, color = BudMuted)
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
        WorkDialog(onDismiss = { addWork = false }) { vm.addWork(it); addWork = false }
    }
    editWork?.let { current ->
        WorkDialog(current = current, onDismiss = { editWork = null }) { vm.addWork(it); editWork = null }
    }
    if (addMaterial) {
        MaterialDialog(onDismiss = { addMaterial = false }) { vm.addMaterial(it); addMaterial = false }
    }
    editMaterial?.let { current ->
        MaterialDialog(current = current, onDismiss = { editMaterial = null }) { vm.addMaterial(it); editMaterial = null }
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
                    SimpleDropdown("Materiał", selected.toString(), materials.map { it.id.toString() to it.name }) { selected = it.toLongOrNull() ?: 0L }
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
                SimpleDropdown("Robota", selected.toString(), works.map { it.id.toString() to it.name }) { selected = it.toLongOrNull() ?: 0L }
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
private fun WorkDialog(current: WorkEntity? = null, onDismiss: () -> Unit, onSave: (WorkEntity) -> Unit) {
    var name by remember(current) { mutableStateOf(current?.name ?: "") }
    var category by remember(current) { mutableStateOf(current?.category ?: "Ogólne") }
    var unit by remember(current) { mutableStateOf(current?.unit ?: "m²") }
    var rate by remember(current) { mutableStateOf(current?.laborRate ?: 0.0) }
    var hours by remember(current) { mutableStateOf(current?.laborHoursPerUnit ?: 0.0) }
    var waste by remember(current) { mutableStateOf(current?.defaultWastePct ?: 10.0) }
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
                onSave(WorkEntity(id = current?.id ?: 0L, name = name.ifBlank { "Nowa robota" }, category = category, unit = unit, laborRate = rate, laborHoursPerUnit = hours, defaultWastePct = waste, active = current?.active ?: true))
            }) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
private fun MaterialDialog(current: MaterialEntity? = null, onDismiss: () -> Unit, onSave: (MaterialEntity) -> Unit) {
    var name by remember(current) { mutableStateOf(current?.name ?: "") }
    var unit by remember(current) { mutableStateOf(current?.unit ?: "szt.") }
    var manufacturer by remember(current) { mutableStateOf(current?.manufacturer ?: "") }
    var cheap by remember(current) { mutableStateOf(current?.priceBudget ?: 0.0) }
    var standard by remember(current) { mutableStateOf(current?.priceStandard ?: 0.0) }
    var premium by remember(current) { mutableStateOf(current?.pricePremium ?: 0.0) }
    var stock by remember(current) { mutableStateOf(current?.stockQty ?: 0.0) }
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
                NumberField("Stan własny", stock, { stock = it })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(MaterialEntity(id = current?.id ?: 0L, name = name.ifBlank { "Nowy materiał" }, unit = unit, manufacturer = manufacturer, priceBudget = cheap, priceStandard = standard, pricePremium = premium, stockQty = stock, active = current?.active ?: true))
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
