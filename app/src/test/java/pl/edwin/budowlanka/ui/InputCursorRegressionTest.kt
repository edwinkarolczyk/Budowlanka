package pl.edwin.budowlanka.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class InputCursorRegressionTest {

    @Test
    fun decimalFilterKeepsCursorAtEditedPosition() {
        val input = TextFieldValue(
            text = "12a,34",
            selection = TextRange(3)
        )

        val result = sanitizeDecimalFieldValue(input)

        assertEquals("12,34", result.text)
        assertEquals(TextRange(2), result.selection)
    }

    @Test
    fun decimalFilterRejectsSecondSeparatorWithoutJumpingToEnd() {
        val input = TextFieldValue(
            text = "12,3.4",
            selection = TextRange(5)
        )

        val result = sanitizeDecimalFieldValue(input)

        assertEquals("12,34", result.text)
        assertEquals(TextRange(4), result.selection)
    }

    @Test
    fun integerFilterKeepsSelectionWhenEditingMiddleOfValue() {
        val input = TextFieldValue(
            text = "12x34",
            selection = TextRange(3)
        )

        val result = sanitizeIntegerFieldValue(input)

        assertEquals("1234", result.text)
        assertEquals(TextRange(2), result.selection)
    }
}
