package com.mfinatti.noclenchingsrs.feature.settings.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mfinatti.noclenchingsrs.domain.settings.ThemeMode
import com.mfinatti.noclenchingsrs.domain.settings.UserSettings
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var savedName: String? = null
    private var theme: ThemeMode? = null
    private var resets = 0

    private fun setContent(settings: UserSettings = UserSettings()) {
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                SettingsScreen(
                    settings = settings,
                    onNameChange = { savedName = it },
                    onThemeChange = { theme = it },
                    onResetConfirmed = { resets++ },
                    onOpenAbout = {},
                )
            }
        }
    }

    @Test
    fun nameField_savesTrimmedTextAfterDebounce() {
        setContent()
        composeRule.onNodeWithTag(SettingsTestTags.NAME_FIELD).performTextInput("  Alex ")
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        assertEquals("Alex", savedName)
    }

    @Test
    fun nameField_isCappedAt24Characters() {
        setContent()
        composeRule.onNodeWithTag(SettingsTestTags.NAME_FIELD).performTextReplacement("A".repeat(30))
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        assertEquals(NAME_MAX_LENGTH, savedName?.length)
        composeRule.onNodeWithText("24/24", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun clearButton_clearsTheName() {
        setContent(UserSettings(name = "Alex"))
        composeRule.onNodeWithTag(SettingsTestTags.NAME_CLEAR).performClick()
        composeRule.mainClock.advanceTimeBy(400)
        composeRule.waitForIdle()
        assertEquals("", savedName)
    }

    @Test
    fun themeButtons_reportTheChoice() {
        setContent()
        composeRule.onNodeWithTag(SettingsTestTags.THEME_DARK).performClick()
        assertEquals(ThemeMode.DARK, theme)
    }

    @Test
    fun resetDialog_cancelDoesNothing_resetConfirms() {
        setContent()
        composeRule.onNodeWithTag(SettingsTestTags.RESET).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.DIALOG_CANCEL).performClick()
        assertEquals(0, resets)
        composeRule.onNodeWithTag(SettingsTestTags.RESET).performClick()
        composeRule.onNodeWithTag(SettingsTestTags.DIALOG_CONFIRM).performClick()
        assertEquals(1, resets)
    }
}
