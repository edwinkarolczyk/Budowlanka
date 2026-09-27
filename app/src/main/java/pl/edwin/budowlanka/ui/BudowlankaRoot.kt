package pl.edwin.budowlanka.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pl.edwin.budowlanka.MainViewModel
import pl.edwin.budowlanka.util.UpdateChecker
import pl.edwin.budowlanka.util.UpdateInfo

private enum class RootScreen(val label: String) {
    DASHBOARD("Pulpit"),
    ESTIMATES("Kosztorysy"),
    CLIENTS("Klienci"),
    CALENDAR("Kalendarz"),
    CATALOG("Materiały"),
    TOOLS("Narzędzia"),
    SETTINGS("Ustawienia"),
    MORE("Więcej")
}

@Composable
fun BudowlankaRoot(vm: MainViewModel) {
    var screenName by rememberSaveable { mutableStateOf(RootScreen.DASHBOARD.name) }
    var estimateId by rememberSaveable { mutableStateOf<Long?>(null) }
    val context = LocalContext.current
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateInfo = UpdateChecker.check(context)
    }

    if (estimateId != null) {
        Box(Modifier.fillMaxSize().background(BudBg)) {
            EstimateEditor(vm = vm, estimateId = estimateId!!, onBack = { estimateId = null })
            if (!updateDismissed) updateInfo?.let { u ->
                UpdateDialog(
                    u,
                    onDownload = { UpdateChecker.download(context, u); updateDismissed = true },
                    onDismiss = { updateDismissed = true }
                )
            }
        }
        return
    }

    val screen = RootScreen.valueOf(screenName)
    val bottomItems = listOf(
        Triple(RootScreen.ESTIMATES, Icons.Rounded.ReceiptLong, "Kosztorysy"),
        Triple(RootScreen.CLIENTS, Icons.Rounded.Groups, "Klienci"),
        Triple(RootScreen.CALENDAR, Icons.Rounded.CalendarMonth, "Kalendarz"),
        Triple(RootScreen.CATALOG, Icons.Rounded.Inventory2, "Materiały"),
        Triple(RootScreen.MORE, Icons.Rounded.MoreHoriz, "Więcej")
    )

    Scaffold(
        containerColor = BudBg,
        topBar = {
            Surface(color = Color(0xFF090C0E), shadowElevation = 8.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { screenName = RootScreen.MORE.name }) {
                        Icon(Icons.Rounded.Menu, "Menu", tint = BudText)
                    }
                    Row(
                        Modifier.weight(1f).clickable { screenName = RootScreen.DASHBOARD.name },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.HomeWork, null, tint = BudOrange, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "BUDOWLANKA",
                                fontWeight = FontWeight.Black,
                                color = BudText,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text("v0.5.3", color = BudMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = { screenName = RootScreen.SETTINGS.name }) {
                        Icon(Icons.Rounded.Settings, "Ustawienia", tint = BudText)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A0D0F), tonalElevation = 8.dp) {
                bottomItems.forEach { (item, icon, label) ->
                    val selected = screen == item
                    NavigationBarItem(
                        selected = selected,
                        onClick = { screenName = item.name },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (selected) BudOrange else BudMuted
                            )
                        },
                        label = {
                            Text(
                                label,
                                color = if (selected) BudOrange else BudMuted,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = BudOrange,
                            selectedTextColor = BudOrange,
                            unselectedIconColor = BudMuted,
                            unselectedTextColor = BudMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize().background(BudBg)) {
            when (screen) {
                RootScreen.DASHBOARD -> DashboardScreen(
                    vm = vm,
                    onOpenEstimate = { estimateId = it },
                    onNewEstimate = {
                        vm.addEstimate("Nowy kosztorys") { estimateId = it }
                    },
                    onClients = { screenName = RootScreen.CLIENTS.name },
                    onCatalog = { screenName = RootScreen.CATALOG.name },
                    onTools = { screenName = RootScreen.TOOLS.name },
                    onCalendar = { screenName = RootScreen.CALENDAR.name },
                    onSettings = { screenName = RootScreen.SETTINGS.name }
                )
                RootScreen.ESTIMATES -> EstimatesScreen(vm, onOpenEstimate = { estimateId = it })
                RootScreen.CLIENTS -> ClientsScreen(vm)
                RootScreen.CALENDAR -> CalendarScreen(vm, onOpenEstimate = { estimateId = it })
                RootScreen.CATALOG -> CatalogScreen(vm)
                RootScreen.TOOLS -> ToolsScreen(vm)
                RootScreen.SETTINGS -> SettingsScreen(vm)
                RootScreen.MORE -> MoreScreen(
                    onDashboard = { screenName = RootScreen.DASHBOARD.name },
                    onCatalog = { screenName = RootScreen.CATALOG.name },
                    onTools = { screenName = RootScreen.TOOLS.name },
                    onSettings = { screenName = RootScreen.SETTINGS.name }
                )
            }
        }
    }

    if (!updateDismissed) updateInfo?.let { u ->
        UpdateDialog(
            u,
            onDownload = { UpdateChecker.download(context, u); updateDismissed = true },
            onDismiss = { updateDismissed = true }
        )
    }
}

@Composable
private fun MoreScreen(
    onDashboard: () -> Unit,
    onCatalog: () -> Unit,
    onTools: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(BudBg).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Więcej", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val items = listOf(
            Triple("Pulpit", Icons.Rounded.Dashboard, onDashboard),
            Triple("Roboty / pakiety / materiały", Icons.Rounded.Wallpaper, onCatalog),
            Triple("Narzędzia", Icons.Rounded.Handyman, onTools),
            Triple("Ustawienia", Icons.Rounded.Settings, onSettings)
        )
        items.forEach { (label, icon, action) ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BudPanel),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable { action() }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, null, tint = BudOrange)
                    Spacer(Modifier.width(14.dp))
                    Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Rounded.ChevronRight, null, tint = BudMuted)
                }
            }
        }
    }
}

@Composable
private fun UpdateDialog(info: UpdateInfo, onDownload: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dostępna aktualizacja ${info.versionName}") },
        text = { Text(info.changelog.ifBlank { "Dostępna jest nowsza wersja aplikacji." }) },
        confirmButton = {
            Button(onClick = onDownload, colors = ButtonDefaults.buttonColors(containerColor = BudOrange)) {
                Text("Pobierz", color = Color.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Później") } }
    )
}
