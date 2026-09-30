package com.mfinatti.noclenchingsrs.feature.home.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun freshInstall_showsGreetingLevelCardStartAndDisclaimer() {
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                HomeScreen(uiState = HomeUiState(), onDismissDisclaimer = {}, onStartClick = {})
            }
        }
        composeRule.onNodeWithTag(HomeTestTags.GREETING).assertTextEquals("Hi there")
        composeRule.onNodeWithText("0 of 3 to Level 2", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Check-ins every 5 min", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.START_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(HomeTestTags.DISCLAIMER_BANNER).assertIsDisplayed()
    }

    @Test
    fun namedUser_isGreetedByName() {
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = true) {
                HomeScreen(uiState = HomeUiState(userName = "Alex"), onDismissDisclaimer = {}, onStartClick = {})
            }
        }
        composeRule.onNodeWithTag(HomeTestTags.GREETING).assertTextEquals("Hi, Alex")
    }

    @Test
    fun gotIt_hidesDisclaimer() {
        var state by mutableStateOf(HomeUiState())
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                HomeScreen(
                    uiState = state,
                    onDismissDisclaimer = { state = state.copy(disclaimerVisible = false) },
                    onStartClick = {},
                )
            }
        }
        composeRule.onNodeWithTag(HomeTestTags.DISCLAIMER_GOT_IT).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(HomeTestTags.DISCLAIMER_BANNER).assertDoesNotExist()
    }
}
