package com.mfinatti.noclenchingsrs.ui.format

import android.content.res.Resources
import com.mfinatti.noclenchingsrs.R
import kotlin.time.Duration

/** "5 min", "45 min", "1 h", "3 h" (US-03 §5). Mixed values fall back to "1 h 30 min". */
fun Resources.formatInterval(interval: Duration): String {
    val minutes = interval.inWholeMinutes
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0L -> getString(R.string.interval_minutes, minutes.toInt())
        rest == 0L -> getString(R.string.interval_hours, hours.toInt())
        else -> getString(R.string.interval_hours_minutes, hours.toInt(), rest.toInt())
    }
}

/** Spelled-out interval for accessibility labels: "15 minutes", "1 hour", "2 hours". */
fun Resources.formatIntervalLong(interval: Duration): String {
    val minutes = interval.inWholeMinutes
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0L -> getQuantityString(R.plurals.interval_minutes_long, minutes.toInt(), minutes.toInt())
        rest == 0L -> getQuantityString(R.plurals.interval_hours_long, hours.toInt(), hours.toInt())
        else -> getQuantityString(R.plurals.interval_hours_long, hours.toInt(), hours.toInt()) + " " +
            getQuantityString(R.plurals.interval_minutes_long, rest.toInt(), rest.toInt())
    }
}
