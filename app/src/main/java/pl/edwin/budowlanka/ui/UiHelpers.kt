package pl.edwin.budowlanka.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

fun money(v: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pl", "PL")).format(v)

@Composable
fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BudPanel),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
fun SimpleDropdown(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = BudMuted)
        Spacer(Modifier.height(3.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (expanded) BudSelectedSoft else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    ),
                border = BorderStroke(
                    if (expanded) 2.dp else 1.dp,
                    if (expanded) BudSelectedStrong else BudLine
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (expanded) BudOrangeLight else BudText
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(options.firstOrNull { it.first == value }?.second ?: value.ifBlank { "Wybierz" })
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { (k, text) ->
                    val selected = k == value
                    DropdownMenuItem(
                        text = {
                            Text(
                                text,
                                color = if (selected) Color.Black else BudText
                            )
                        },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Rounded.Check, null, tint = Color.Black) }
                        } else null,
                        colors = MenuDefaults.itemColors(),
                        modifier = Modifier.background(
                            if (selected) BudOrangeStrong else Color.Transparent
                        ),
                        onClick = {
                            expanded = false
                            onSelected(k)
                        }
                    )
                }
            }
        }
    }
}

private fun displayNumber(value: Double): String {
    if (value == 0.0) return ""
    val raw = if (value % 1.0 == 0.0) value.toLong().toString()
    else value.toString().trimEnd('0').trimEnd('.')
    return raw.replace('.', ',')
}

@Composable
fun NumberField(
    label: String,
    value: Double,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf(displayNumber(value)) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) text = displayNumber(value)
    }

    OutlinedTextField(
        value = text,
        onValueChange = { entered ->
            val filtered = buildString {
                var separatorSeen = false
                entered.forEach { ch ->
                    when {
                        ch.isDigit() -> append(ch)
                        (ch == ',' || ch == '.') && !separatorSeen -> {
                            append(ch)
                            separatorSeen = true
                        }
                    }
                }
            }
            text = filtered
            if (filtered.isBlank()) onValue(0.0)
            else filtered.replace(',', '.').toDoubleOrNull()?.let(onValue)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BudOrangeStrong,
            focusedLabelColor = BudOrangeLight,
            cursorColor = BudOrangeStrong,
            focusedContainerColor = BudSelectedSoft,
            unfocusedBorderColor = BudLine
        ),
        modifier = modifier.onFocusChanged { state ->
            focused = state.isFocused
            if (!state.isFocused) {
                text = displayNumber(text.replace(',', '.').toDoubleOrNull() ?: value)
            }
        }
    )
}

@Composable
fun IntField(
    label: String,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(if (value == 0) "" else value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter { c -> c.isDigit() }
            onValue(text.toIntOrNull() ?: 0)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BudOrangeStrong,
            focusedLabelColor = BudOrangeLight,
            cursorColor = BudOrangeStrong,
            focusedContainerColor = BudSelectedSoft,
            unfocusedBorderColor = BudLine
        ),
        modifier = modifier
    )
}
