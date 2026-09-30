package com.mfinatti.noclenchingsrs.debugtools.qa

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.debugtools.DebugTools
import com.mfinatti.noclenchingsrs.di.AppContainer
import com.mfinatti.noclenchingsrs.feature.settings.ui.SettingsGroup
import com.mfinatti.noclenchingsrs.ui.theme.Spacing

internal class QaDebugTools(
    private val container: AppContainer,
) : DebugTools {

    override val isAvailable: Boolean = true

    @Composable
    override fun SettingsEntry(onClick: () -> Unit) {
        QaSettingsEntry(onClick = onClick)
    }

    @Composable
    override fun Panel(onBack: () -> Unit) {
        QaPanelRoute(container = container, onBack = onBack)
    }
}

/** Last group in Settings (US-02 §2). */
@Composable
internal fun QaSettingsEntry(onClick: () -> Unit) {
    SettingsGroup(title = stringResource(R.string.qa_settings_group)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.qa_entry_title)) },
            supportingContent = { Text(stringResource(R.string.qa_entry_supporting)) },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_bug),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            trailingContent = {
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .heightIn(min = 72.dp)
                .clickable(onClick = onClick)
                .testTag("settings_qa"),
        )
    }
}
