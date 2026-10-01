package com.mfinatti.noclenchingsrs.feature.checkin.ring

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.mfinatti.noclenchingsrs.R
import com.mfinatti.noclenchingsrs.ui.components.BannerAction
import com.mfinatti.noclenchingsrs.ui.components.InfoBanner
import com.mfinatti.noclenchingsrs.ui.components.InfoBannerVariant

object RingBannerTags {
    const val FSI_BANNER = "banner_fsi"
    const val FSI_ALLOW = "banner_fsi_allow"
    const val SETTINGS_EXACT_BANNER = "settings_banner_exact"
    const val SETTINGS_EXACT_ALLOW = "settings_banner_exact_allow"
}

/** US-11 §2.1.2 / §5: "Ring can't show full screen" + Allow full screen (Android 14+). */
@Composable
fun FullScreenDeniedBanner(onAllow: () -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(R.string.banner_fsi_title)
    val body = stringResource(R.string.banner_fsi_body)
    InfoBanner(
        title = title,
        body = body,
        icon = R.drawable.ic_fullscreen,
        variant = InfoBannerVariant.Attention,
        a11yDescription = "$title. $body",
        actions = listOf(
            BannerAction(
                label = stringResource(R.string.banner_fsi_action),
                onClick = onAllow,
                testTag = RingBannerTags.FSI_ALLOW,
            ),
        ),
        modifier = modifier.testTag(RingBannerTags.FSI_BANNER),
    )
}

/** US-11 AC10: the US-05 exact-timing banner, shown under Alert style when Ring is selected. */
@Composable
fun ExactTimingSettingsBanner(onAllow: () -> Unit, modifier: Modifier = Modifier) {
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
                label = stringResource(R.string.banner_exact_action),
                onClick = onAllow,
                testTag = RingBannerTags.SETTINGS_EXACT_ALLOW,
            ),
        ),
        modifier = modifier.testTag(RingBannerTags.SETTINGS_EXACT_BANNER),
    )
}

/** System pages behind the Ring banners. */
object RingSettingsLinks {
    fun openFullScreenSettings(context: Context) {
        try {
            context.startActivity(FullScreenStatus.settingsIntent(context))
        } catch (e: ActivityNotFoundException) {
            openAppDetails(context)
        }
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        try {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (e: ActivityNotFoundException) {
            openAppDetails(context)
        }
    }

    private fun openAppDetails(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
