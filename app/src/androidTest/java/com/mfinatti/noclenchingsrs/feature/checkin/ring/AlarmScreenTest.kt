package com.mfinatti.noclenchingsrs.feature.checkin.ring

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** US-11 §3: alarm screen answers and the pocket guard. */
@RunWith(AndroidJUnit4::class)
class AlarmScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var answered: Answer? = null
    private var silences = 0

    private fun setScreen(guardMillis: Long = 0L, silenced: Boolean = false, confirmation: AlarmConfirmation? = null) {
        composeRule.setContent {
            SuperUnclenchTheme(darkTheme = false) {
                AlarmScreen(
                    level = 3,
                    silenced = silenced,
                    confirmation = confirmation,
                    onAnswer = { answered = it },
                    onSilence = { silences++ },
                    guardMillis = guardMillis,
                )
            }
        }
    }

    @Test
    fun goodAndBadAnswer() {
        setScreen()
        composeRule.onNodeWithTag(AlarmTestTags.QUESTION).assertIsDisplayed()
        composeRule.onNodeWithTag(AlarmTestTags.GOOD).performClick()
        assertEquals(Answer.GOOD, answered)
        composeRule.onNodeWithTag(AlarmTestTags.BAD).performClick()
        assertEquals(Answer.BAD, answered)
    }

    @Test
    fun guard_ignoresTapsInTheFirst800ms() {
        composeRule.mainClock.autoAdvance = false
        setScreen(guardMillis = ALARM_GUARD_MILLIS)
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithTag(AlarmTestTags.GOOD).performTouchInput {
            down(center)
            up()
        }
        composeRule.mainClock.advanceTimeBy(50)
        assertNull("tap during the guard window must not answer", answered)
        composeRule.mainClock.advanceTimeBy(ALARM_GUARD_MILLIS)
        composeRule.onNodeWithTag(AlarmTestTags.GOOD).performTouchInput {
            down(center)
            up()
        }
        composeRule.mainClock.advanceTimeBy(50)
        assertEquals(Answer.GOOD, answered)
    }

    @Test
    fun guard_accessibilityClickBypassesTheGuard() {
        composeRule.mainClock.autoAdvance = false
        setScreen(guardMillis = ALARM_GUARD_MILLIS)
        composeRule.mainClock.advanceTimeBy(100)
        composeRule.onNodeWithTag(AlarmTestTags.BAD).performSemanticsAction(SemanticsActions.OnClick)
        assertEquals(Answer.BAD, answered)
    }

    @Test
    fun guard_aSwipeAcrossATileDoesNotAnswer() {
        setScreen()
        composeRule.onNodeWithTag(AlarmTestTags.GOOD).performTouchInput { swipeRight() }
        assertNull(answered)
    }

    @Test
    fun guard_aSecondPointerCancels() {
        setScreen()
        composeRule.onNodeWithTag(AlarmTestTags.GOOD).performTouchInput {
            down(0, center)
            down(1, Offset(center.x + 20f, center.y + 20f))
            up(1)
            up(0)
        }
        assertNull(answered)
    }

    @Test
    fun guard_releaseOutsideTheTileDoesNotAnswer() {
        setScreen()
        composeRule.onNodeWithTag(AlarmTestTags.GOOD).performTouchInput {
            down(Offset(width - 4f, center.y))
            moveTo(Offset(width + 10f, center.y))
            up()
        }
        assertNull(answered)
    }

    @Test
    fun silence_thenSilencedState() {
        setScreen()
        composeRule.onNodeWithTag(AlarmTestTags.SILENCE).performClick()
        assertEquals(1, silences)
    }

    @Test
    fun silencedState_disablesSilenceAndShowsWaitingLine() {
        setScreen(silenced = true)
        composeRule.onNodeWithTag(AlarmTestTags.SILENCE).assertIsNotEnabled()
        composeRule.onNodeWithText("Still waiting for your answer.").assertIsDisplayed()
    }

    @Test
    fun confirmation_replacesTheTiles() {
        setScreen(confirmation = AlarmConfirmation(Answer.GOOD, "Level up! Check-ins now every 10 minutes."))
        composeRule.onNodeWithTag(AlarmTestTags.CONFIRMATION).assertIsDisplayed()
        composeRule.onNodeWithTag(AlarmTestTags.GOOD).assertDoesNotExist()
        composeRule.onNodeWithText("Nice — noted.").assertIsDisplayed()
        assertTrue(answered == null)
    }
}
