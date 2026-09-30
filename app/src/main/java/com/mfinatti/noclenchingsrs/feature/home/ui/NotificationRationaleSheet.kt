package com.mfinatti.noclenchingsrs.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme
import kotlinx.coroutines.launch

/** Which rationale to show (US-03 design §3). */
enum class RationaleMode {
    /** Permission can still be requested: Continue opens the system dialog. */
    ASK,

    /** Permanently denied: the primary button opens the app's notification settings. */
    BLOCKED,
}

object RationaleSheetTestTags {
    const val SHEET = "sheet_rationale"
    const val CONTINUE = "sheet_continue"
    const val NOT_NOW = "sheet_not_now"
}

/**
 * Notification-permission rationale bottom sheet. Tap-only: "Not now" is always available because
 * QA automation can't swipe the sheet away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationRationaleSheet(
    mode: RationaleMode,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun hideThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        RationaleSheetContent(
            mode = mode,
            onPrimary = { hideThen(onPrimary) },
            onNotNow = { hideThen(onDismiss) },
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun RationaleSheetContent(
    mode: RationaleMode,
    onPrimary: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The sheet is its own window: re-enable resource-id test tags for UI Automator.
            .semantics { testTagsAsResourceId = true }
            .testTag(RationaleSheetTestTags.SHEET)
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_notifications),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(R.string.rationale_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = stringResource(
                if (mode == RationaleMode.ASK) R.string.rationale_body else R.string.rationale_body_blocked,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.primaryCtaHeight)
                .testTag(RationaleSheetTestTags.CONTINUE),
        ) {
            Text(
                text = stringResource(
                    if (mode == RationaleMode.ASK) R.string.rationale_continue else R.string.rationale_open_settings,
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        TextButton(
            onClick = onNotNow,
            modifier = Modifier
                .heightIn(min = Dimens.minTouchTarget)
                .testTag(RationaleSheetTestTags.NOT_NOW),
        ) {
            Text(text = stringResource(R.string.rationale_not_now), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(name = "Rationale - ask - light", showBackground = true)
@Composable
private fun RationaleAskPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            RationaleSheetContent(mode = RationaleMode.ASK, onPrimary = {}, onNotNow = {})
        }
    }
}

@Preview(name = "Rationale - blocked - dark", showBackground = true)
@Composable
private fun RationaleBlockedPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
            RationaleSheetContent(mode = RationaleMode.BLOCKED, onPrimary = {}, onNotNow = {})
        }
    }
}
