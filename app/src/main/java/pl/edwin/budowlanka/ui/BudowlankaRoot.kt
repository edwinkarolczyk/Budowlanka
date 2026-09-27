package pl.edwin.budowlanka.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import pl.edwin.budowlanka.util.UpdateChecker
import pl.edwin.budowlanka.util.UpdateInfo
import pl.edwin.budowlanka.MainViewModel

private enum class RootScreen(val label: String) {
    DASHBOARD("Pulpit"),
    ESTIMATES("Wyceny"),
    CATALOG("Baza"),
    CLIENTS("Klienci"),
    CALENDAR("Kalendarz"),
    TOOLS("Narzędzia"),
    SETTINGS("Ustawienia")
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
        Box {
            EstimateEditor(
                vm = vm,
                estimateId = estimateId!!,
                onBack = { estimateId = null }
            )
            if (!updateDismissed) updateInfo?.let { u ->
                UpdateDialog(u, onDownload = { UpdateChecker.download(context, u); updateDismissed = true }, onDismiss = { updateDismissed = true })
            }
        }
        return
    }

    val screen = RootScreen.valueOf(screenName)
    Scaffold(
        topBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Budowlanka", style = MaterialTheme.typography.titleLarge)
                        Text("wersja rozwojowa 0.5.0", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(screen.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 5.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RootScreen.entries.forEach { item ->
                        TextButton(
                            onClick = { screenName = item.name },
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 6.dp)
                        ) {
                            Text(item.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (screen) {
                RootScreen.DASHBOARD -> DashboardScreen(vm, onOpenEstimate = { estimateId = it })
                RootScreen.ESTIMATES -> EstimatesScreen(vm, onOpenEstimate = { estimateId = it })
                RootScreen.CATALOG -> CatalogScreen(vm)
                RootScreen.CLIENTS -> ClientsScreen(vm)
                RootScreen.CALENDAR -> CalendarScreen(vm, onOpenEstimate = { estimateId = it })
                RootScreen.TOOLS -> ToolsScreen(vm)
                RootScreen.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
    if (!updateDismissed) updateInfo?.let { u ->
        UpdateDialog(u, onDownload = { UpdateChecker.download(context, u); updateDismissed = true }, onDismiss = { updateDismissed = true })
    }
}

@Composable
private fun UpdateDialog(info: UpdateInfo, onDownload: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dostępna aktualizacja ${info.versionName}") },
        text = { Text(info.changelog.ifBlank { "Dostępna jest nowsza wersja aplikacji." }) },
        confirmButton = { Button(onClick = onDownload) { Text("Pobierz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Później") } }
    )
}
