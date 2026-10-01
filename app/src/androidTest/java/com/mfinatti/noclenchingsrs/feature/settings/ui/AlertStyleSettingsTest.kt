package com.mfinatti.noclenchingsrs.feature.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mfinatti.noclenchingsrs.domain.settings.AlertStyle
import com.mfinatti.noclenchingsrs.feature.checkin.ring.RingBannerTags
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** US-11 §2: Alert style tiles, Ring note and banners. */
@RunWith(AndroidJUnit4::class)
class AlertStyleSettingsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nudgeByDefault_ringSelectsAndShowsNote() {
        var style by mutableStateOf(AlertStyle.NUDGE)
        val changes = mutableListOf<AlertStyle>()
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                Column {
                    AlertStyleSettings(
                        style = style,
                        fullScreenDenied = false,
                        exactDenied = false,
                        onStyleChange = {
                            changes += it
                            style = it
                        },
                        onAllowFullScreen = {},
                        onAllowExact = {},
                    )
                }
            }
        }
        composeRule.onNodeWithTag(AlertStyleTestTags.NUDGE).assertIsSelected()
        composeRule.onNodeWithTag(AlertStyleTestTags.RING_NOTE).assertDoesNotExist()
        composeRule.onNodeWithTag(AlertStyleTestTags.RING).performClick()
        composeRule.onNodeWithTag(AlertStyleTestTags.RING).assertIsSelected()
        composeRule.onNodeWithTag(AlertStyleTestTags.NUDGE).assertIsNotSelected()
        composeRule.onNodeWithTag(AlertStyleTestTags.RING_NOTE).assertIsDisplayed()
        composeRule.onNodeWithTag(RingBannerTags.FSI_BANNER).assertDoesNotExist()
        assertEquals(listOf(AlertStyle.RING), changes)
    }

    @Test
    fun ringWithPermissionsMissing_showsBothBanners() {
        var fsiClicks = 0
        var exactClicks = 0
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                Column {
                    AlertStyleSettings(
                        style = AlertStyle.RING,
                        fullScreenDenied = true,
                        exactDenied = true,
                        onStyleChange = {},
                        onAllowFullScreen = { fsiClicks++ },
                        onAllowExact = { exactClicks++ },
                    )
                }
            }
        }
        composeRule.onNodeWithTag(RingBannerTags.FSI_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(RingBannerTags.FSI_ALLOW).performClick()
        composeRule.onNodeWithTag(RingBannerTags.SETTINGS_EXACT_BANNER).assertIsDisplayed()
        composeRule.onNodeWithTag(RingBannerTags.SETTINGS_EXACT_ALLOW).performClick()
        assertEquals(1, fsiClicks)
        assertEquals(1, exactClicks)
    }
}
