package com.mfinatti.noclenchingsrs.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** US-10 (reopened): Starts/Ends rows and the time picker dialog. */
@RunWith(AndroidJUnit4::class)
class QuietHoursSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var range: Pair<Int, Int>? = null

    private fun setRows(enabled: Boolean = true, start: Int = 22 * 60, end: Int = 7 * 60) {
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                Column {
                    QuietHoursSettings(
                        enabled = enabled,
                        startMinutes = start,
                        endMinutes = end,
                        onEnabledChange = {},
                        onRangeChange = { s, e -> range = s to e },
                    )
                }
            }
        }
    }

    @Test
    fun overnightRange_showsNextDayAndDuration() {
        setRows(start = 23 * 60 + 30, end = 6 * 60 + 15)
        composeRule.onNodeWithText("Next day", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(QuietTestTags.DURATION).assertTextEquals("6 h 45 min of quiet")
    }

    @Test
    fun sameDayRange_hasNoNextDayCaption() {
        setRows(start = 13 * 60, end = 14 * 60)
        composeRule.onNodeWithText("Next day", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(QuietTestTags.DURATION).assertTextEquals("1 h of quiet")
    }

    @Test
    fun switchOff_rowsDisabled() {
        setRows(enabled = false)
        composeRule.onNodeWithTag(QuietTestTags.STARTS).assertIsNotEnabled()
        composeRule.onNodeWithTag(QuietTestTags.ENDS).assertIsNotEnabled()
        composeRule.onNodeWithTag(QuietTestTags.STARTS).performClick()
        composeRule.onNodeWithTag(QuietTestTags.PICKER).assertDoesNotExist()
    }

    @Test
    fun picker_okSavesAndCancelDoesNot() {
        setRows()
        composeRule.onNodeWithTag(QuietTestTags.ENDS).performClick()
        composeRule.onNodeWithTag(QuietTestTags.PICKER).assertIsDisplayed()
        composeRule.onNodeWithTag(QuietTestTags.PICKER_CANCEL).performClick()
        composeRule.onNodeWithTag(QuietTestTags.PICKER).assertDoesNotExist()
        assertNull(range)

        composeRule.onNodeWithTag(QuietTestTags.STARTS).performClick()
        composeRule.onNodeWithTag(QuietTestTags.PICKER_OK).assertIsEnabled().performClick()
        composeRule.onNodeWithTag(QuietTestTags.PICKER).assertDoesNotExist()
        assertEquals(22 * 60 to 7 * 60, range) // unchanged value, saved through OK
    }

    @Test
    fun picker_sameAsOtherEnd_showsErrorAndDisablesOk() {
        var confirmed: Int? = null
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                QuietTimePickerDialog(
                    which = QuietEnd.END,
                    initialMinutes = 22 * 60,
                    otherMinutes = 22 * 60,
                    onConfirm = { confirmed = it },
                    onDismiss = {},
                )
            }
        }
        composeRule.onNodeWithTag(QuietTestTags.PICKER_ERROR).assertTextEquals("Start and end can't be the same")
        composeRule.onNodeWithTag(QuietTestTags.PICKER_OK).assertIsNotEnabled().performClick()
        assertNull(confirmed)
        // The keyboard toggle switches to text input and back.
        composeRule.onNodeWithTag(QuietTestTags.PICKER_MODE_TOGGLE).performClick()
        composeRule.onNodeWithTag(QuietTestTags.PICKER_MODE_TOGGLE).performClick()
        composeRule.onNodeWithTag(QuietTestTags.PICKER).assertIsDisplayed()
    }
}
