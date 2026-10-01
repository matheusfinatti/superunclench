package com.mfinatti.noclenchingsrs.feature.checkin.ring

import android.provider.Settings
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import com.mfinatti.noclenchingsrs.ui.theme.tabular
import kotlinx.coroutines.delay
import java.util.Locale

/** Test tags from docs/design/US-11-alert-style.md §12. */
object AlarmTestTags {
    const val SCREEN = "alarm_screen"
    const val TIME = "alarm_time"
    const val LEVEL = "alarm_level"
    const val QUESTION = "alarm_question"
    const val GOOD = "alarm_good"
    const val BAD = "alarm_bad"
    const val SILENCE = "alarm_silence"
    const val CONFIRMATION = "alarm_confirmation"
}

/** Answer confirmation shown for 1.2 s (2 s with a level line) before the screen closes (§3.4). */
data class AlarmConfirmation(val answer: Answer, val levelLine: String? = null)

/** Taps on the answer tiles are ignored this long after the screen appears (pocket guard, §3.2). */
const val ALARM_GUARD_MILLIS = 800L

/**
 * US-11 §3 alarm screen: the time, level pill, a breathing circle, "Is your jaw relaxed?" and two
 * large guarded Good/Bad tiles in the thumb zone, plus Silence.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AlarmScreen(
    level: Int,
    silenced: Boolean,
    confirmation: AlarmConfirmation?,
    onAnswer: (Answer) -> Unit,
    onSilence: () -> Unit,
    modifier: Modifier = Modifier,
    nowMillis: () -> Long = System::currentTimeMillis,
    guardMillis: Long = ALARM_GUARD_MILLIS,
) {
    val colors = MaterialTheme.colorScheme
    val pane = stringResource(R.string.alarm_pane_title)
    val reducedMotion = rememberReducedMotion()
    val breath = if (reducedMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "breathing")
        val value by transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(BREATH_MILLIS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "breath",
        )
        value
    }
    val glowColor = colors.primaryContainer
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .drawBehind {
                // Soft radial glow behind the breathing circle (§3.1); alpha follows the breath.
                val alpha = 0.45f + (breath - 0.92f) / 0.08f * 0.15f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor.copy(alpha = alpha), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.36f),
                        radius = size.width * 0.7f,
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width / 2f, size.height * 0.36f),
                )
            }
            .semantics {
                paneTitle = pane
                testTagsAsResourceId = true
            }
            .testTag(AlarmTestTags.SCREEN),
    ) {
        val landscape = maxHeight < 600.dp && maxWidth > maxHeight
        val fontScale = LocalDensity.current.fontScale
        val stackTiles = fontScale >= 1.5f || (maxWidth - 48.dp - 12.dp) / 2 < 160.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (landscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Header(level = level, nowMillis = nowMillis, compactTime = true)
                        Spacer(Modifier.height(16.dp))
                        Question()
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AnswersOrConfirmation(
                            confirmation = confirmation,
                            stacked = true,
                            tileHeight = 96.dp,
                            silenced = silenced,
                            guardMillis = guardMillis,
                            onAnswer = onAnswer,
                            onSilence = onSilence,
                        )
                    }
                }
            } else {
                // Scrolls only when it must (large fonts); otherwise the answers sit at the bottom,
                // in the thumb zone (SpaceBetween over at least the screen height).
                val available = this@BoxWithConstraints.maxHeight
                Column(
                    modifier = Modifier
                        .widthIn(max = 480.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = available - 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Header(level = level, nowMillis = nowMillis, compactTime = fontScale >= 1.5f)
                    Spacer(Modifier.height(24.dp))
                    if (!stackTiles) {
                        BreathingCircle(scale = breath)
                        Spacer(Modifier.height(24.dp))
                    }
                    Question()
                    Spacer(Modifier.height(24.dp))
                  }
                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnswersOrConfirmation(
                        confirmation = confirmation,
                        stacked = stackTiles,
                        tileHeight = if (stackTiles) 112.dp else 128.dp,
                        silenced = silenced,
                        guardMillis = guardMillis,
                        onAnswer = onAnswer,
                        onSilence = onSilence,
                    )
                    Spacer(Modifier.height(16.dp))
                  }
                }
            }
        }
    }
}

@Composable
private fun Header(level: Int, nowMillis: () -> Long, compactTime: Boolean) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    // Ticks on the minute.
    val now by produceState(initialValue = nowMillis()) {
        while (true) {
            val current = nowMillis()
            value = current
            delay(MINUTE_MILLIS - current % MINUTE_MILLIS + 50)
        }
    }
    val date = java.util.Date(now)
    val time = DateFormat.getTimeFormat(context).format(date)
    val shortDate = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEMMMd"), date).toString()
    val longDate = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd"), date).toString()
    Text(
        text = stringResource(R.string.alarm_header),
        style = MaterialTheme.typography.labelLarge,
        color = colors.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clearAndSetSemantics { contentDescription = "$time, $longDate" }
            .testTag(AlarmTestTags.TIME),
    ) {
        Text(
            text = time,
            style = (if (compactTime) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge).tabular(),
            color = colors.onSurface,
        )
        Text(text = shortDate, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant)
    }
    Spacer(Modifier.height(16.dp))
    val names = stringArrayResource(R.array.level_names)
    Surface(shape = CircleShape, color = colors.surfaceContainerHigh) {
        Text(
            text = stringResource(R.string.alarm_level, level, names.getOrElse(level - 1) { "" }),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag(AlarmTestTags.LEVEL),
        )
    }
}

@Composable
private fun BreathingCircle(scale: Float) {
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminanceIsDark()
    Box(
        modifier = Modifier
            .size(168.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(colors.primaryContainer)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sentiment_calm),
            contentDescription = null,
            tint = if (dark) colors.onPrimaryContainer else colors.primary,
            modifier = Modifier.size(72.dp),
        )
    }
}

@Composable
private fun Question() {
    Text(
        text = stringResource(R.string.alarm_question),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .semantics { heading() }
            .testTag(AlarmTestTags.QUESTION),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.alarm_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AnswersOrConfirmation(
    confirmation: AlarmConfirmation?,
    stacked: Boolean,
    tileHeight: androidx.compose.ui.unit.Dp,
    silenced: Boolean,
    guardMillis: Long,
    onAnswer: (Answer) -> Unit,
    onSilence: () -> Unit,
) {
    AnimatedContent(
        targetState = confirmation,
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "answer",
        modifier = Modifier.fillMaxWidth(),
    ) { shown ->
        if (shown != null) {
            Confirmation(shown)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                AnswerTiles(stacked = stacked, tileHeight = tileHeight, guardMillis = guardMillis, onAnswer = onAnswer)
                Spacer(Modifier.height(16.dp))
                SilenceControl(silenced = silenced, onSilence = onSilence)
            }
        }
    }
}

@Composable
private fun AnswerTiles(stacked: Boolean, tileHeight: androidx.compose.ui.unit.Dp, guardMillis: Long, onAnswer: (Answer) -> Unit) {
    // Pocket guard rule 1: inputs ignored (and tiles fading in) for the first 800 ms.
    val reveal = remember { Animatable(if (guardMillis <= 0L) 1f else 0f) }
    var armed by remember { mutableStateOf(guardMillis <= 0L) }
    LaunchedEffect(Unit) {
        if (guardMillis > 0L) {
            reveal.animateTo(1f, tween(guardMillis.toInt()))
            armed = true
        }
    }
    val extended = SuperUnclenchTheme.extendedColors
    val good: @Composable (Modifier) -> Unit = { m ->
        AlarmAnswerTile(
            label = stringResource(R.string.answer_good),
            caption = stringResource(R.string.answer_good_caption),
            a11yLabel = stringResource(R.string.answer_good_a11y),
            icon = R.drawable.ic_good,
            container = extended.good,
            content = extended.onGood,
            armed = armed,
            onClick = { onAnswer(Answer.GOOD) },
            modifier = m
                .height(tileHeight)
                .graphicsLayer { alpha = reveal.value }
                .testTag(AlarmTestTags.GOOD),
        )
    }
    val bad: @Composable (Modifier) -> Unit = { m ->
        AlarmAnswerTile(
            label = stringResource(R.string.answer_bad),
            caption = stringResource(R.string.answer_bad_caption),
            a11yLabel = stringResource(R.string.answer_bad_a11y),
            icon = R.drawable.ic_bad,
            container = extended.bad,
            content = extended.onBad,
            armed = armed,
            onClick = { onAnswer(Answer.BAD) },
            modifier = m
                .height(tileHeight)
                .graphicsLayer { alpha = reveal.value }
                .testTag(AlarmTestTags.BAD),
        )
    }
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            good(Modifier.fillMaxWidth())
            bad(Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            good(Modifier.weight(1f))
            bad(Modifier.weight(1f))
        }
    }
}

/**
 * design-system §9.7 answer tile: a big rounded target. Touch counts only when [armed], on release
 * inside the tile, with a single pointer and less than 24dp of travel (pocket guard rules 1–3).
 * The accessibility click bypasses the guard (an explicit action).
 */
@Composable
fun AlarmAnswerTile(
    label: String,
    caption: String,
    a11yLabel: String,
    @DrawableRes icon: Int,
    container: Color,
    content: Color,
    armed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val currentOnClick by rememberUpdatedState(onClick)
    val currentArmed by rememberUpdatedState(armed)
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(100), label = "tilePress")
    fun accept() {
        view.performHapticFeedback(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.VIRTUAL_KEY
            },
        )
        currentOnClick()
    }
    Box(
        modifier = modifier
            .scale(scale)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(if (pressed) lerpPressed(container, content) else container)
            .semantics {
                role = Role.Button
                contentDescription = a11yLabel
                onClick {
                    accept()
                    true
                }
            }
            .pointerInput(Unit) {
                val slop = 24.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!currentArmed) return@awaitEachGesture
                    pressed = true
                    var cancelled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        // A second finger / palm / fabric touch cancels the gesture.
                        if (event.changes.any { it.id != down.id && it.pressed }) cancelled = true
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if ((change.position - down.position).getDistance() > slop) cancelled = true
                        if (!change.pressed) {
                            val inside = change.position.x in 0f..size.width.toFloat() &&
                                change.position.y in 0f..size.height.toFloat()
                            if (!cancelled && inside) {
                                change.consume()
                                accept()
                            }
                            break
                        }
                        if (cancelled) pressed = false
                    }
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(12.dp)) {
            Icon(painter = painterResource(icon), contentDescription = null, tint = content, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium), color = content)
            Text(
                caption,
                style = MaterialTheme.typography.bodyMedium,
                color = content.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun lerpPressed(container: Color, content: Color): Color =
    androidx.compose.ui.graphics.lerp(container, content, 0.12f)

@Composable
private fun SilenceControl(silenced: Boolean, onSilence: () -> Unit) {
    val silenceA11y = stringResource(R.string.alarm_silence_a11y)
    val silencedText = stringResource(R.string.alarm_silenced)
    TextButton(
        onClick = onSilence,
        enabled = !silenced,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics { contentDescription = if (silenced) silencedText else silenceA11y }
            .testTag(AlarmTestTags.SILENCE),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_volume_off),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(if (silenced) silencedText else stringResource(R.string.alarm_silence))
    }
    if (silenced) {
        Text(
            text = stringResource(R.string.alarm_still_waiting),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Confirmation(confirmation: AlarmConfirmation) {
    val extended = SuperUnclenchTheme.extendedColors
    val good = confirmation.answer == Answer.GOOD
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
            .semantics(mergeDescendants = true) {}
            .testTag(AlarmTestTags.CONFIRMATION),
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(if (good) extended.goodContainer else extended.badContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(if (good) R.drawable.ic_good else R.drawable.ic_bad),
                contentDescription = null,
                tint = if (good) extended.onGoodContainer else extended.onBadContainer,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(if (good) R.string.answered_good else R.string.answered_bad),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        confirmation.levelLine?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private fun Color.luminanceIsDark(): Boolean = (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

private const val BREATH_MILLIS = 4_000
private const val MINUTE_MILLIS = 60_000L

@Preview(name = "Alarm — light", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun AlarmScreenLightPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        AlarmScreen(level = 3, silenced = false, confirmation = null, onAnswer = {}, onSilence = {}, guardMillis = 0L)
    }
}

@Preview(name = "Alarm — dark, silenced", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun AlarmScreenDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        AlarmScreen(level = 3, silenced = true, confirmation = null, onAnswer = {}, onSilence = {}, guardMillis = 0L)
    }
}

@Preview(name = "Alarm — answered", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun AlarmScreenAnsweredPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        AlarmScreen(
            level = 3,
            silenced = false,
            confirmation = AlarmConfirmation(Answer.GOOD, "Level up! Check-ins now every 10 min."),
            onAnswer = {},
            onSilence = {},
            guardMillis = 0L,
        )
    }
}

@Preview(name = "Alarm — landscape", widthDp = 915, heightDp = 412, showBackground = true)
@Composable
private fun AlarmScreenLandscapePreview() {
    SuperUnclenchTheme(darkTheme = false) {
        AlarmScreen(level = 3, silenced = false, confirmation = null, onAnswer = {}, onSilence = {}, guardMillis = 0L)
    }
}
