package com.mfinatti.noclenchingsrs.feature.checkin.ring

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.SuperUnclenchApplication
import com.mfinatti.noclenchingsrs.domain.checkin.CheckInSource
import com.mfinatti.noclenchingsrs.domain.session.SessionState
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.domain.srs.LevelChange
import com.mfinatti.noclenchingsrs.feature.checkin.domain.AnswerResult
import com.mfinatti.noclenchingsrs.ui.format.formatIntervalLong
import com.mfinatti.noclenchingsrs.ui.theme.AppNightMode
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * US-11 §3 alarm screen. Opened by the ringing notification's full-screen intent (locked / screen
 * off) or by tapping its body. Shows over the lock screen and turns the screen on; answering needs
 * no unlock. Closes itself as soon as the check-in it was opened for is no longer ringing (answered
 * elsewhere, capped, Stop/Pause), and never shows answer buttons for a resolved check-in.
 */
class AlarmActivity : ComponentActivity() {

    private var checkInAt by mutableLongStateOf(NO_CHECK_IN)
    private var preview by mutableStateOf(false)
    private var confirmation by mutableStateOf<AlarmConfirmation?>(null)

    /** Set before the answer is recorded, so the "ring resolved" emission doesn't close us early. */
    private var answering by mutableStateOf(false)

    private val container get() = (application as SuperUnclenchApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            AppNightMode.windowTheme(AppNightMode.mirrored(this))?.let { setTheme(it) }
        }
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()
        // Pocket guard rule 4: ignore touches while another window covers ours.
        window.decorView.filterTouchesWhenObscured = true
        // Announced by TalkBack when the window appears (§10).
        title = getString(R.string.alarm_pane_title)
        readIntent(intent)
        setContent {
            val settings by container.settingsState.collectAsStateWithLifecycle()
            val session by container.sessionRepository.session.collectAsStateWithLifecycle<SessionState?>(initialValue = null)
            val loadedSession = session
            val loadedSettings = settings ?: return@setContent
            // A real alarm screen only lives while its check-in rings (or is silenced).
            LaunchedEffect(loadedSession, preview, answering) {
                if (!preview && !answering && loadedSession != null && !loadedSession.isRingingFor(checkInAt)) {
                    finishAndRemoveTask()
                }
            }
            if (!preview && loadedSession == null) return@setContent
            SuperUnclenchTheme(themeMode = loadedSettings.themeMode) {
                AlarmScreen(
                    level = loadedSession?.srs?.level ?: 1,
                    silenced = !preview && loadedSession?.ringSilenced == true,
                    confirmation = confirmation,
                    onAnswer = ::answer,
                    onSilence = ::silence,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        confirmation = null
        answering = false
        readIntent(intent)
    }

    /** Volume keys silence the ring (US-11 AC6); the screen stays until answered. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE -> {
            silence()
            true
        }
        else -> super.onKeyDown(keyCode, event)
    }

    private fun readIntent(intent: Intent?) {
        checkInAt = intent?.getLongExtra(EXTRA_CHECK_IN_AT, NO_CHECK_IN) ?: NO_CHECK_IN
        preview = intent?.getBooleanExtra(EXTRA_PREVIEW, false) == true
    }

    private fun answer(answer: Answer) {
        if (confirmation != null || answering) return
        answering = true
        if (preview) {
            confirmation = AlarmConfirmation(answer)
            lifecycleScope.launch {
                delay(CONFIRMATION_MILLIS)
                finishAndRemoveTask()
            }
            return
        }
        lifecycleScope.launch {
            val result = container.checkInController.answer(answer, checkInAt, CheckInSource.ALARM_SCREEN)
            if (result !is AnswerResult.Applied) {
                finishAndRemoveTask()
                return@launch
            }
            val line = levelLine(result)
            confirmation = AlarmConfirmation(answer, line)
            delay(if (line != null) CONFIRMATION_LEVEL_MILLIS else CONFIRMATION_MILLIS)
            finishAndRemoveTask()
        }
    }

    private fun levelLine(result: AnswerResult.Applied): String? {
        val interval = resources.formatIntervalLong(container.srsEngine.interval(result.srs.level))
        return when (result.change) {
            LevelChange.PROMOTED -> getString(R.string.alarm_level_up_line, interval)
            LevelChange.DEMOTED -> getString(R.string.msg_demote, result.srs.level, interval)
            else -> null
        }
    }

    private fun silence() {
        if (preview) return
        lifecycleScope.launch { container.checkInController.silenceRing() }
    }

    private fun SessionState.isRingingFor(checkIn: Long): Boolean =
        ringActive && pendingCheckInAtMillis == checkIn

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        // Keeps the alarm visible while it rings (≤ 10 min cap); the power key still turns it off.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_CHECK_IN_AT = "check_in_at"
        const val EXTRA_PREVIEW = "preview"
        private const val NO_CHECK_IN = -1L
        private const val CONFIRMATION_MILLIS = 1_200L
        private const val CONFIRMATION_LEVEL_MILLIS = 2_000L

        fun intent(context: Context, checkInAtMillis: Long): Intent =
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
                .putExtra(EXTRA_CHECK_IN_AT, checkInAtMillis)
                .setData(Uri.parse("superunclench://alarm/$checkInAtMillis"))

        /** Debug "Preview alarm screen": no sound, nothing recorded (US-11 §11). */
        fun previewIntent(context: Context): Intent =
            Intent(context, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_PREVIEW, true)
                .setData(Uri.parse("superunclench://alarm/preview"))
    }
}
