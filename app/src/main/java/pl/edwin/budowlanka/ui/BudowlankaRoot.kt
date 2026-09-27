package pl.edwin.budowlanka.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    if (estimateId != null) {
        EstimateEditor(
            vm = vm,
            estimateId = estimateId!!,
            onBack = { estimateId = null }
        )
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
}
