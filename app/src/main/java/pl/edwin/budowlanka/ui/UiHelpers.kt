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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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

private fun remapSelectionAfterFilter(
    source: TextFieldValue,
    keptIndexes: List<Int>,
    outputLength: Int
): TextRange {
    fun mapped(position: Int): Int =
        keptIndexes.count { it < position }.coerceIn(0, outputLength)

    return TextRange(
        start = mapped(source.selection.start),
        end = mapped(source.selection.end)
    )
}

internal fun sanitizeDecimalFieldValue(input: TextFieldValue): TextFieldValue {
    val output = StringBuilder()
    val keptIndexes = mutableListOf<Int>()
    var separatorSeen = false

    input.text.forEachIndexed { index, ch ->
        val keep = when {
            ch.isDigit() -> true
            (ch == ',' || ch == '.') && !separatorSeen -> {
                separatorSeen = true
                true
            }
            else -> false
        }
        if (keep) {
            keptIndexes += index
            output.append(ch)
        }
    }

    val text = output.toString()
    return TextFieldValue(
        text = text,
        selection = remapSelectionAfterFilter(input, keptIndexes, text.length)
    )
}

internal fun sanitizeIntegerFieldValue(input: TextFieldValue): TextFieldValue {
    val output = StringBuilder()
    val keptIndexes = mutableListOf<Int>()

    input.text.forEachIndexed { index, ch ->
        if (ch.isDigit()) {
            keptIndexes += index
            output.append(ch)
        }
    }

    val text = output.toString()
    return TextFieldValue(
        text = text,
        selection = remapSelectionAfterFilter(input, keptIndexes, text.length)
    )
}

@Composable
fun NumberField(
    label: String,
    value: Double,
    onValue: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var fieldValue by remember {
        val initial = displayNumber(value)
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) {
            val displayed = displayNumber(value)
            if (fieldValue.text != displayed) {
                fieldValue = TextFieldValue(displayed, selection = TextRange(displayed.length))
            }
        }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { entered ->
            val sanitized = sanitizeDecimalFieldValue(entered)
            fieldValue = sanitized

            if (sanitized.text.isBlank()) {
                onValue(0.0)
            } else {
                sanitized.text.replace(',', '.').toDoubleOrNull()?.let(onValue)
            }
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
                val normalized = displayNumber(
                    fieldValue.text.replace(',', '.').toDoubleOrNull() ?: value
                )
                fieldValue = TextFieldValue(
                    normalized,
                    selection = TextRange(normalized.length)
                )
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
    var fieldValue by remember {
        val initial = if (value == 0) "" else value.toString()
        mutableStateOf(TextFieldValue(initial, selection = TextRange(initial.length)))
    }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) {
            val displayed = if (value == 0) "" else value.toString()
            if (fieldValue.text != displayed) {
                fieldValue = TextFieldValue(displayed, selection = TextRange(displayed.length))
            }
        }
    }

    OutlinedTextField(
        value = fieldValue,
        onValueChange = { entered ->
            val sanitized = sanitizeIntegerFieldValue(entered)
            fieldValue = sanitized
            onValue(sanitized.text.toIntOrNull() ?: 0)
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
        modifier = modifier.onFocusChanged { state ->
            focused = state.isFocused
            if (!state.isFocused) {
                val normalized = (fieldValue.text.toIntOrNull() ?: value)
                    .toString()
                    .let { if (it == "0") "" else it }
                fieldValue = TextFieldValue(
                    normalized,
                    selection = TextRange(normalized.length)
                )
            }
        }
    )
}
