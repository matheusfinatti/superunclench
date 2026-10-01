package com.mfinatti.noclenchingsrs.feature.home.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import com.mfinatti.noclenchingsrs.ui.components.StatTile
import com.mfinatti.noclenchingsrs.ui.components.StatTileKind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mfinatti.noclenchingsrs.R
import androidx.compose.material3.TextButton
import com.mfinatti.noclenchingsrs.feature.checkin.ring.RingSettingsLinks
import com.mfinatti.noclenchingsrs.feature.checkin.ring.FullScreenStatus
import com.mfinatti.noclenchingsrs.feature.checkin.ring.FullScreenDeniedBanner
import com.mfinatti.noclenchingsrs.di.LocalAppContainer
import com.mfinatti.noclenchingsrs.domain.session.SessionStatus
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.feature.checkin.notification.NotificationStatus
import com.mfinatti.noclenchingsrs.ui.components.AnswerButtons
import com.mfinatti.noclenchingsrs.ui.components.AppSnackbarHost
import com.mfinatti.noclenchingsrs.ui.components.BannerAction
import com.mfinatti.noclenchingsrs.ui.components.InfoBanner
import com.mfinatti.noclenchingsrs.ui.components.InfoBannerVariant
import com.mfinatti.noclenchingsrs.ui.components.StatusChip
import com.mfinatti.noclenchingsrs.ui.format.formatInterval
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import kotlin.time.Duration.Companion.minutes

/** Test tags from docs/design/US-01-app-shell.md §8 and US-03-start-and-checkin.md §9. */
object HomeTestTags {
    const val GREETING = "home_greeting"
    const val DISCLAIMER_BANNER = "disclaimer_banner"
    const val DISCLAIMER_GOT_IT = "disclaimer_got_it"
    const val LEVEL_CARD = "level_card"
    const val START_BUTTON = "btn_start"
    const val STOP_BUTTON = "btn_stop"
    const val STATUS_CHIP = "status_chip"
    const val PENDING_CARD = "pending_card"
    const val PENDING_MISSED_HINT = "pending_missed_hint"
    const val TODAY_ROW = "home_today_row"
    const val NOTIF_OFF_BANNER = "banner_notif_off"
    const val NOTIF_OFF_SETTINGS = "banner_notif_off_settings"
    const val PENDING_RINGING_OVERLINE = "pending_ringing_overline"
    const val PENDING_SILENCE = "pending_silence"
    const val EXACT_BANNER = "banner_exact"
    const val EXACT_ALLOW = "banner_exact_allow"
    const val EXACT_NOT_NOW = "banner_exact_not_now"
    const val NOTIF_OFF_CAPTION = "session_notif_off_caption"
}

private const val BANNER_ANIM_MILLIS = 300
private const val SESSION_CROSSFADE_MILLIS = 150
private const val ANSWER_CONFIRMATION_MILLIS = 1_200L
private const val USER_RESUME_WINDOW_MILLIS = 2_000L

@Composable
fun HomeRoute(onOpenStats: () -> Unit = {}) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            settingsRepository = container.settingsRepository,
            sessionRepository = container.sessionRepository,
            debugOverridesRepository = container.debugOverridesRepository,
            checkInController = container.checkInController,
            checkInLogRepository = container.checkInLogRepository,
            srsEngine = container.srsEngine,
        )
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Re-checked on every resume (e.g. returning from system notification settings).
    var canDeliver by remember { mutableStateOf(NotificationStatus.canDeliverCheckIns(context)) }
    var canScheduleExact by remember { mutableStateOf(container.alarmScheduler.canScheduleExactAlarms()) }
    var canUseFullScreen by remember { mutableStateOf(FullScreenStatus.canUseFullScreenIntent(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canUseFullScreen = FullScreenStatus.canUseFullScreenIntent(context)
        canDeliver = NotificationStatus.canDeliverCheckIns(context)
        canScheduleExact = container.alarmScheduler.canScheduleExactAlarms()
    }

    var rationaleMode by rememberSaveable { mutableStateOf<RationaleMode?>(null) }
    // Start; when starting inside quiet hours, say when the first check-in will come (US-10 §3.2).
    fun startSession() {
        scope.launch {
            val started = viewModel.start()
            val next = started.nextAlarmAtMillis
            if (uiState?.quiet?.isActive(System.currentTimeMillis()) == true && next != null) {
                val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(next))
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(resources.getString(R.string.quiet_start_snackbar, time))
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        canDeliver = NotificationStatus.canDeliverCheckIns(context)
        // Allow -> the session starts. Don't allow -> stays stopped; the banner explains why.
        if (granted) startSession()
    }

    val openNotificationSettings: () -> Unit = {
        try {
            context.startActivity(NotificationStatus.appNotificationSettingsIntent(context))
        } catch (e: ActivityNotFoundException) {
            // No settings screen available (very unusual); nothing else to do.
        }
    }

    val openExactAlarmSettings: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                .setData(Uri.fromParts("package", context.packageName, null))
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // Fall back to the app's details screen.
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", context.packageName, null)),
                )
            }
        }
    }

    val onStartClick: () -> Unit = start@{
        val current = uiState ?: return@start
        val usesRuntimePermission = NotificationStatus.requiresRuntimePermission &&
            !current.previewLegacyNotificationPermission
        val granted = NotificationStatus.isPermissionGranted(context)
        if (usesRuntimePermission && (!granted || current.previewNotificationsOff)) {
            val activity = context.findActivity()
            val systemWillAsk = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
            // Asked before and the system no longer shows the dialog => permanently denied.
            val blocked = !granted && current.notificationPermissionRequested && !systemWillAsk
            rationaleMode = if (blocked) RationaleMode.BLOCKED else RationaleMode.ASK
        } else {
            startSession()
        }
    }

    val onStopClick: () -> Unit = {
        viewModel.onStop()
        val message = resources.getString(R.string.snackbar_stopped)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    fun showSnackbar(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // Set while a user-initiated Resume is in flight, so only automatic resumes get the snackbar.
    var userResumedAt by remember { mutableLongStateOf(0L) }
    val onPause: (PauseChoice) -> Unit = { choice ->
        scope.launch {
            val paused = viewModel.pause(choice.duration)
            val until = paused.pausedUntilMillis
            showSnackbar(
                if (until != null) {
                    resources.getString(
                        R.string.snackbar_paused_until,
                        android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(until)),
                    )
                } else {
                    resources.getString(R.string.snackbar_paused_indef)
                },
            )
        }
    }
    val onResume: () -> Unit = {
        userResumedAt = System.currentTimeMillis()
        viewModel.onResume()
        // Resuming from auto-pause confirms with a snackbar (US-07 §2).
        if (uiState?.autoPaused == true) showSnackbar(resources.getString(R.string.snackbar_resumed))
    }
    // Timed pause ended while Home is open → "Check-ins resumed" (US-06 §3).
    var lastStatus by remember { mutableStateOf<SessionStatus?>(null) }
    val currentStatus = uiState?.sessionStatus
    LaunchedEffect(currentStatus) {
        val previous = lastStatus
        lastStatus = currentStatus
        val automatic = System.currentTimeMillis() - userResumedAt > USER_RESUME_WINDOW_MILLIS
        if (previous == SessionStatus.PAUSED && currentStatus == SessionStatus.RUNNING && automatic) {
            showSnackbar(resources.getString(R.string.snackbar_resumed))
        }
    }

    val state = uiState
    HomeScreen(
        uiState = state,
        showNotificationsOffBanner = state?.showNotificationsOffBanner(
            canDeliver = canDeliver,
            requiresRuntimePermission = NotificationStatus.requiresRuntimePermission,
        ) ?: false,
        showExactTimingBanner = state?.showExactTimingBanner(canScheduleExact) ?: false,
        showFullScreenBanner = state?.showFullScreenBanner(canUseFullScreen) ?: false,
        onAllowFullScreen = { RingSettingsLinks.openFullScreenSettings(context) },
        onSilenceRing = viewModel::onSilenceRing,
        notificationsBlocked = !canDeliver || state?.previewNotificationsOff == true,
        onAllowExactTiming = openExactAlarmSettings,
        onDismissExactTiming = viewModel::onDismissExactTimingBanner,
        snackbarHostState = snackbarHostState,
        onDismissDisclaimer = viewModel::onDismissDisclaimer,
        onStartClick = onStartClick,
        onStopClick = onStopClick,
        onPause = onPause,
        onResume = onResume,
        onAnswer = viewModel::onAnswer,
        onOpenNotificationSettings = openNotificationSettings,
        onOpenStats = onOpenStats,
    )

    val mode = rationaleMode
    if (mode != null) {
        NotificationRationaleSheet(
            mode = mode,
            onPrimary = {
                rationaleMode = null
                if (mode == RationaleMode.ASK) {
                    viewModel.onNotificationPermissionRequested()
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    openNotificationSettings()
                }
            },
            onDismiss = { rationaleMode = null },
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Home. Slot order is fixed: A banners -> B pending card -> C level card -> D session card ->
 * E today summary (E arrives in US-08).
 *
 * @param uiState null while persisted state is loading; only the top bar is drawn then.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState?,
    onDismissDisclaimer: () -> Unit,
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    showNotificationsOffBanner: Boolean = false,
    showExactTimingBanner: Boolean = false,
    showFullScreenBanner: Boolean = false,
    onAllowFullScreen: () -> Unit = {},
    onSilenceRing: () -> Unit = {},
    notificationsBlocked: Boolean = false,
    onAllowExactTiming: () -> Unit = {},
    onDismissExactTiming: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onStopClick: () -> Unit = {},
    onPause: (PauseChoice) -> Unit = {},
    onResume: () -> Unit = {},
    onAnswer: (Answer, Long) -> Unit = { _, _ -> },
    onOpenNotificationSettings: () -> Unit = {},
    onOpenStats: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.semantics { heading() },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        snackbarHost = { AppSnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (uiState != null) {
            HomeContent(
                uiState = uiState,
                showNotificationsOffBanner = showNotificationsOffBanner,
                showExactTimingBanner = showExactTimingBanner,
                showFullScreenBanner = showFullScreenBanner,
                onAllowFullScreen = onAllowFullScreen,
                onSilenceRing = onSilenceRing,
                notificationsBlocked = notificationsBlocked,
                onAllowExactTiming = onAllowExactTiming,
                onDismissExactTiming = onDismissExactTiming,
                onDismissDisclaimer = onDismissDisclaimer,
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onPause = onPause,
                onResume = onResume,
                onAnswer = onAnswer,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenStats = onOpenStats,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    showNotificationsOffBanner: Boolean,
    showExactTimingBanner: Boolean,
    showFullScreenBanner: Boolean,
    onAllowFullScreen: () -> Unit,
    onSilenceRing: () -> Unit,
    notificationsBlocked: Boolean,
    onAllowExactTiming: () -> Unit,
    onDismissExactTiming: () -> Unit,
    onDismissDisclaimer: () -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onPause: (PauseChoice) -> Unit,
    onResume: () -> Unit,
    onAnswer: (Answer, Long) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenStats: () -> Unit,
    contentPadding: PaddingValues,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.TopCenter,
    ) {
        val gutter = if (maxWidth >= 600.dp) Dimens.gutterWide else Dimens.gutterCompact
        val expandedWidth = maxWidth >= 840.dp
        Column(
            modifier = Modifier
                .widthIn(max = Dimens.maxContentWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = gutter)
                .padding(top = Spacing.lg, bottom = Dimens.bottomContentPadding),
        ) {
            Greeting(
                userName = uiState.userName,
                status = uiState.sessionStatus,
                hasPendingCheckIn = uiState.pendingCheckInAtMillis != null,
                quietActive = uiState.quiet.isActive(System.currentTimeMillis()),
            )
            Spacer(Modifier.height(Dimens.sectionGap))

            // Slot A: banners, priority order (notifications off, then disclaimer).
            AnimatedVisibility(
                visible = showNotificationsOffBanner,
                enter = expandVertically(tween(BANNER_ANIM_MILLIS)) + fadeIn(tween(BANNER_ANIM_MILLIS)),
                exit = shrinkVertically(tween(BANNER_ANIM_MILLIS)) + fadeOut(tween(BANNER_ANIM_MILLIS)),
            ) {
                NotificationsOffBanner(
                    onOpenSettings = onOpenNotificationSettings,
                    modifier = Modifier.padding(bottom = Dimens.cardGap),
                )
            }
            // US-11 §5: priority 2, after notifications-off and before exact timing.
            AnimatedVisibility(
                visible = showFullScreenBanner,
                enter = expandVertically(tween(BANNER_ANIM_MILLIS)) + fadeIn(tween(BANNER_ANIM_MILLIS)),
                exit = shrinkVertically(tween(BANNER_ANIM_MILLIS)) + fadeOut(tween(BANNER_ANIM_MILLIS)),
            ) {
                FullScreenDeniedBanner(
                    onAllow = onAllowFullScreen,
                    modifier = Modifier.padding(bottom = Dimens.cardGap),
                )
            }
            AnimatedVisibility(
                visible = showExactTimingBanner,
                enter = expandVertically(tween(BANNER_ANIM_MILLIS)) + fadeIn(tween(BANNER_ANIM_MILLIS)),
                exit = shrinkVertically(tween(BANNER_ANIM_MILLIS)) + fadeOut(tween(BANNER_ANIM_MILLIS)),
            ) {
                ExactTimingBanner(
                    onAllow = onAllowExactTiming,
                    onNotNow = onDismissExactTiming,
                    modifier = Modifier.padding(bottom = Dimens.cardGap),
                )
            }
            // At most two banners (US-01 §2.1.3): the disclaimer waits behind the other two.
            AnimatedVisibility(
                visible = uiState.disclaimerVisible &&
                    listOf(showNotificationsOffBanner, showFullScreenBanner, showExactTimingBanner).count { it } < 2,
                exit = shrinkVertically(tween(BANNER_ANIM_MILLIS)) + fadeOut(tween(BANNER_ANIM_MILLIS)),
            ) {
                DisclaimerBanner(
                    onGotIt = onDismissDisclaimer,
                    modifier = Modifier.padding(bottom = Dimens.cardGap),
                )
            }

            // Slot B: pending check-in.
            PendingCheckInSlot(
                pendingCheckInAtMillis = uiState.pendingCheckInAtMillis,
                showMissedHint = uiState.consecutiveMisses >= 1,
                onAnswer = onAnswer,
                ringing = uiState.ringing,
                ringSilenced = uiState.ringSilenced,
                onSilence = onSilenceRing,
            )

            // Slot C: level card (US-04).
            LevelCard(
                level = uiState.level,
                subLevel = uiState.subLevel,
                subLevelCount = uiState.subLevelCount,
                maxLevel = uiState.maxLevel,
                interval = uiState.interval,
                lastAnswer = uiState.lastAnswer,
                expandedWidth = expandedWidth,
                shortInterval = uiState.shortInterval,
            )
            Spacer(Modifier.height(Dimens.cardGap))

            // Slot D: session card.
            SessionCard(
                status = uiState.sessionStatus,
                nextAlarmAtMillis = uiState.nextAlarmAtMillis,
                pausedUntilMillis = uiState.pausedUntilMillis,
                autoPaused = uiState.autoPaused,
                quiet = uiState.quiet,
                ringing = uiState.ringing || uiState.ringSilenced,
                notificationsBlocked = notificationsBlocked,
                intervalLabel = LocalResources.current.formatInterval(uiState.interval),
                onStartClick = onStartClick,
                onStopClick = onStopClick,
                onPause = onPause,
                onResume = onResume,
            )

            // Slot E: today summary (US-08 §8).
            Spacer(Modifier.height(Dimens.sectionGap))
            TodaySummary(
                good = uiState.todayGood,
                bad = uiState.todayBad,
                missed = uiState.todayMissed,
                onClick = onOpenStats,
            )
        }
    }
}

@Composable
private fun Greeting(userName: String?, status: SessionStatus, hasPendingCheckIn: Boolean, quietActive: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = if (userName.isNullOrBlank()) {
                stringResource(R.string.home_greeting_default)
            } else {
                stringResource(R.string.home_greeting_named, userName)
            },
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .testTag(HomeTestTags.GREETING)
                .semantics { heading() },
        )
        Text(
            text = stringResource(
                when {
                    hasPendingCheckIn -> R.string.home_subtitle_pending
                    status == SessionStatus.RUNNING && quietActive -> R.string.home_subtitle_quiet
                    status == SessionStatus.RUNNING -> R.string.home_subtitle_running
                    status == SessionStatus.PAUSED -> R.string.home_subtitle_paused
                    else -> R.string.home_subtitle_stopped
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NotificationsOffBanner(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    InfoBanner(
        body = stringResource(R.string.banner_notif_off),
        icon = R.drawable.ic_notifications_off,
        variant = InfoBannerVariant.Attention,
        actions = listOf(
            BannerAction(
                label = stringResource(R.string.banner_open_settings),
                onClick = onOpenSettings,
                testTag = HomeTestTags.NOTIF_OFF_SETTINGS,
            ),
        ),
        modifier = modifier.testTag(HomeTestTags.NOTIF_OFF_BANNER),
    )
}

@Composable
private fun ExactTimingBanner(onAllow: () -> Unit, onNotNow: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.banner_exact_title)
    val body = stringResource(R.string.banner_exact_body)
    InfoBanner(
        title = title,
        body = body,
        icon = R.drawable.ic_alarm,
        variant = InfoBannerVariant.Attention,
        a11yDescription = "$title. $body",
        actions = listOf(
            BannerAction(
                label = stringResource(R.string.banner_not_now),
                onClick = onNotNow,
                testTag = HomeTestTags.EXACT_NOT_NOW,
            ),
            BannerAction(
                label = stringResource(R.string.banner_exact_action),
                onClick = onAllow,
                testTag = HomeTestTags.EXACT_ALLOW,
            ),
        ),
        modifier = modifier.testTag(HomeTestTags.EXACT_BANNER),
    )
}

@Composable
private fun DisclaimerBanner(onGotIt: () -> Unit, modifier: Modifier = Modifier) {
    val body = stringResource(R.string.disclaimer_body)
    InfoBanner(
        body = body,
        icon = R.drawable.ic_info,
        variant = InfoBannerVariant.Neutral,
        a11yDescription = stringResource(R.string.disclaimer_a11y, body),
        actions = listOf(
            BannerAction(
                label = stringResource(R.string.disclaimer_action),
                onClick = onGotIt,
                testTag = HomeTestTags.DISCLAIMER_GOT_IT,
            ),
        ),
        modifier = modifier.testTag(HomeTestTags.DISCLAIMER_BANNER),
    )
}

/** An answer tapped on the card, kept on screen for [ANSWER_CONFIRMATION_MILLIS]. */
private data class AnswerConfirmation(val answer: Answer, val checkInAtMillis: Long)

@Composable
private fun PendingCheckInSlot(
    pendingCheckInAtMillis: Long?,
    showMissedHint: Boolean,
    onAnswer: (Answer, Long) -> Unit,
    ringing: Boolean = false,
    ringSilenced: Boolean = false,
    onSilence: () -> Unit = {},
) {
    var confirmation by remember { mutableStateOf<AnswerConfirmation?>(null) }
    LaunchedEffect(confirmation) {
        if (confirmation != null) {
            delay(ANSWER_CONFIRMATION_MILLIS)
            confirmation = null
        }
    }
    val cardAt: Long? = confirmation?.checkInAtMillis ?: pendingCheckInAtMillis
    // Keep the last time on screen while the card animates out.
    var displayedAt by remember { mutableLongStateOf(pendingCheckInAtMillis ?: 0L) }
    LaunchedEffect(cardAt) {
        if (cardAt != null) displayedAt = cardAt
    }
    AnimatedVisibility(
        visible = cardAt != null,
        enter = expandVertically(tween(BANNER_ANIM_MILLIS)) + fadeIn(tween(BANNER_ANIM_MILLIS)),
        exit = shrinkVertically(tween(BANNER_ANIM_MILLIS)) + fadeOut(tween(BANNER_ANIM_MILLIS)),
    ) {
        PendingCheckInCard(
            checkInAtMillis = cardAt ?: displayedAt,
            answered = confirmation?.answer,
            showMissedHint = showMissedHint,
            ringing = ringing,
            ringSilenced = ringSilenced,
            onSilence = onSilence,
            onAnswer = { answer ->
                if (cardAt != null && confirmation == null) {
                    confirmation = AnswerConfirmation(answer, cardAt)
                    onAnswer(answer, cardAt)
                }
            },
            modifier = Modifier.padding(bottom = Dimens.cardGap),
        )
    }
}

@Composable
private fun PendingCheckInCard(
    checkInAtMillis: Long,
    answered: Answer?,
    onAnswer: (Answer) -> Unit,
    modifier: Modifier = Modifier,
    showMissedHint: Boolean = false,
    ringing: Boolean = false,
    ringSilenced: Boolean = false,
    onSilence: () -> Unit = {},
) {
    val context = LocalContext.current
    val time = remember(checkInAtMillis) {
        android.text.format.DateFormat.getTimeFormat(context).format(Date(checkInAtMillis))
    }
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val borderColor by animateColorAsState(
        targetValue = if (answered == null) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(SESSION_CROSSFADE_MILLIS),
        label = "pendingBorder",
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(HomeTestTags.PENDING_CARD),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = onContainer,
        ),
        // The primary outline fades out while the answer confirmation shows (US-03 design follow-up 2).
        border = BorderStroke(1.dp, borderColor),
    ) {
        Crossfade(
            targetState = answered,
            animationSpec = tween(SESSION_CROSSFADE_MILLIS),
            label = "pendingCard",
        ) { answer ->
            if (answer == null) {
                val announcement = stringResource(R.string.pending_a11y_announcement)
                Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                    Column(
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = announcement
                        },
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(if (ringing) R.drawable.ic_alarm else R.drawable.ic_stat_checkin),
                                contentDescription = null,
                                modifier = Modifier.size(if (ringing) 16.dp else 18.dp),
                            )
                            Spacer(Modifier.size(Spacing.sm))
                            Text(
                                text = stringResource(
                                    if (ringing) R.string.pending_overline_ringing else R.string.pending_overline,
                                    time,
                                ).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                modifier = if (ringing) Modifier.testTag(HomeTestTags.PENDING_RINGING_OVERLINE) else Modifier,
                            )
                        }
                        Text(
                            text = stringResource(R.string.pending_title),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = stringResource(R.string.pending_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onContainer.copy(alpha = 0.8f),
                        )
                        if (ringSilenced) {
                            Text(
                                text = stringResource(R.string.pending_silenced_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = onContainer.copy(alpha = 0.7f),
                            )
                        }
                        if (showMissedHint) {
                            // US-07 §3: reassuring, never guilt-inducing.
                            Text(
                                text = stringResource(R.string.pending_missed_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = onContainer.copy(alpha = 0.7f),
                                modifier = Modifier.testTag(HomeTestTags.PENDING_MISSED_HINT),
                            )
                        }
                    }
                    if (ringing) {
                        // US-11 §5: Silence, right-aligned under the captions.
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = onSilence,
                                modifier = Modifier.testTag(HomeTestTags.PENDING_SILENCE),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_volume_off),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(Spacing.sm))
                                Text(stringResource(R.string.alarm_silence))
                            }
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                    AnswerButtons(onAnswer = onAnswer)
                }
            } else {
                AnswerConfirmationRow(answer = answer)
            }
        }
    }
}

@Composable
private fun AnswerConfirmationRow(answer: Answer) {
    val extended = SuperUnclenchTheme.extendedColors
    val good = answer == Answer.GOOD
    Surface(
        color = if (good) extended.goodContainer else extended.badContainer,
        contentColor = if (good) extended.onGoodContainer else extended.onBadContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(Dimens.cardPadding)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(if (good) R.drawable.ic_good else R.drawable.ic_bad),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.size(Spacing.md))
            Text(
                text = stringResource(if (good) R.string.answered_good else R.string.answered_bad),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun HomeCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(modifier = Modifier.padding(Dimens.cardPadding)) {
            content()
        }
    }
}

/** Home slot E: TODAY overline + Good / Bad / Missed tiles; the whole row opens Stats. */
@Composable
private fun TodaySummary(good: Int, bad: Int, missed: Int, onClick: () -> Unit) {
    val a11y = stringResource(R.string.home_today_a11y, good, bad, missed)
    val stacked = LocalDensity.current.fontScale >= 1.5f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(HomeTestTags.TODAY_ROW)
            .clearAndSetSemantics {
                contentDescription = a11y
                role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.home_today_overline).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val tiles: List<@Composable (Modifier) -> Unit> = listOf(
            { m -> StatTile(stringResource(R.string.tile_good), good.toString(), R.drawable.ic_good, StatTileKind.GOOD, m, compact = true) },
            { m -> StatTile(stringResource(R.string.tile_bad), bad.toString(), R.drawable.ic_bad, StatTileKind.BAD, m, compact = true) },
            { m ->
                StatTile(stringResource(R.string.tile_missed), missed.toString(), R.drawable.ic_missed, StatTileKind.MISSED, m, compact = true)
            },
        )
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) { tiles.forEach { it(Modifier.fillMaxWidth()) } }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) { tiles.forEach { it(Modifier.weight(1f)) } }
        }
    }
}

// region Previews

private val PreviewFirstLaunch = HomeUiState()
private val PreviewNamed = HomeUiState(userName = "Alex", disclaimerVisible = false)
private val PreviewRunningPending = HomeUiState(
    userName = "Alex",
    disclaimerVisible = false,
    sessionStatus = SessionStatus.RUNNING,
    level = 3,
    subLevel = 2,
    subLevelCount = 4,
    interval = 15.minutes,
    pendingCheckInAtMillis = 1_790_000_000_000L,
)

@Preview(name = "Home - first launch - light", showBackground = true)
@Composable
private fun HomeFirstLaunchLightPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        HomeScreen(uiState = PreviewFirstLaunch, onDismissDisclaimer = {}, onStartClick = {})
    }
}

@Preview(name = "Home - first launch - dark", showBackground = true)
@Composable
private fun HomeFirstLaunchDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        HomeScreen(uiState = PreviewFirstLaunch, onDismissDisclaimer = {}, onStartClick = {})
    }
}

@Preview(name = "Home - named, disclaimer dismissed - dark", showBackground = true)
@Composable
private fun HomeNamedDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        HomeScreen(uiState = PreviewNamed, onDismissDisclaimer = {}, onStartClick = {})
    }
}

@Preview(name = "Home - running, pending, notifications off - light", showBackground = true, heightDp = 900)
@Composable
private fun HomeRunningPendingLightPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        HomeScreen(
            uiState = PreviewRunningPending,
            showNotificationsOffBanner = true,
            onDismissDisclaimer = {},
            onStartClick = {},
        )
    }
}

@Preview(name = "Home - running, pending - dark", showBackground = true, heightDp = 900)
@Composable
private fun HomeRunningPendingDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        HomeScreen(uiState = PreviewRunningPending, onDismissDisclaimer = {}, onStartClick = {})
    }
}

@Preview(name = "Home - 200% font", showBackground = true, fontScale = 2f)
@Composable
private fun HomeLargeFontPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        HomeScreen(uiState = PreviewFirstLaunch, onDismissDisclaimer = {}, onStartClick = {})
    }
}

@PreviewScreenSizes
@Composable
private fun HomeScreenSizesPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        HomeScreen(uiState = PreviewFirstLaunch, onDismissDisclaimer = {}, onStartClick = {})
    }
}

// endregion
