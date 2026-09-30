package com.mfinatti.noclenchingsrs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.domain.srs.Answer
import com.mfinatti.noclenchingsrs.ui.theme.Dimens
import com.mfinatti.noclenchingsrs.ui.theme.Spacing
import com.mfinatti.noclenchingsrs.ui.theme.SuperUnclenchTheme

object AnswerButtonTestTags {
    const val GOOD = "btn_answer_good"
    const val BAD = "btn_answer_bad"
}

/** design-system §9.2 `AnswerButtons`: Good (left, green) / Bad (right, red), 56dp, with captions. */
@Composable
fun AnswerButtons(
    onAnswer: (Answer) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val extended = SuperUnclenchTheme.extendedColors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        AnswerButton(
            label = stringResource(R.string.answer_good),
            caption = stringResource(R.string.answer_good_caption),
            a11yLabel = stringResource(R.string.answer_good_a11y),
            icon = R.drawable.ic_good,
            container = extended.good,
            content = extended.onGood,
            testTag = AnswerButtonTestTags.GOOD,
            enabled = enabled,
            onClick = { onAnswer(Answer.GOOD) },
            modifier = Modifier.weight(1f),
        )
        AnswerButton(
            label = stringResource(R.string.answer_bad),
            caption = stringResource(R.string.answer_bad_caption),
            a11yLabel = stringResource(R.string.answer_bad_a11y),
            icon = R.drawable.ic_bad,
            container = extended.bad,
            content = extended.onBad,
            testTag = AnswerButtonTestTags.BAD,
            enabled = enabled,
            onClick = { onAnswer(Answer.BAD) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AnswerButton(
    label: String,
    caption: String,
    a11yLabel: String,
    icon: Int,
    container: Color,
    content: Color,
    testTag: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = content,
                disabledContainerColor = container.copy(alpha = 0.38f),
                disabledContentColor = content.copy(alpha = 0.38f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.primaryCtaHeight)
                .testTag(testTag)
                .semantics { contentDescription = a11yLabel },
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(Dimens.buttonIconSize),
            )
            Spacer(Modifier.size(Spacing.sm))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

@Preview(name = "Answer buttons - light", showBackground = true)
@Composable
private fun AnswerButtonsPreview() {
    SuperUnclenchTheme(darkTheme = false) {
        Surface { AnswerButtons(onAnswer = {}, modifier = Modifier.padding(Spacing.lg)) }
    }
}

@Preview(name = "Answer buttons - dark", showBackground = true)
@Composable
private fun AnswerButtonsDarkPreview() {
    SuperUnclenchTheme(darkTheme = true) {
        Surface { AnswerButtons(onAnswer = {}, modifier = Modifier.padding(Spacing.lg)) }
    }
}
