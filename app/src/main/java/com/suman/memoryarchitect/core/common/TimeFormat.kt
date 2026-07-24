package com.suman.memoryarchitect.core.common

/** Formats a millisecond duration as a one-decimal seconds label, e.g. 4230L -> "4.2s". */
fun Long.toSecondsLabel(): String {
    val tenths = this / 100
    return "%d.%ds".format(tenths / 10, tenths % 10)
}

/** Formats a (potentially large) millisecond duration for a lifetime-totals context - "3h 24m",
 * "45m", or "12s" for anything under a minute - unlike [toSecondsLabel], which is built for a
 * single round's completion time and would read as absurd precision ("12340.0s") once the
 * duration is measured in hours (Statistics Dashboard's Total Play Time). */
fun Long.toPlayTimeLabel(): String {
    val totalSeconds = this / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/** Zero-padded "H:MM:SS" (hours unpadded past two digits, e.g. "168:00:00" for a fresh weekly
 * lock) remaining until [unlockAtEpochSecond] as of [nowEpochSecond], clamped to zero once
 * passed — feeds Mode Select's post-win Daily/Weekly Challenge lock countdown. */
fun remainingHms(unlockAtEpochSecond: Long, nowEpochSecond: Long): String {
    val remainingSeconds = (unlockAtEpochSecond - nowEpochSecond).coerceAtLeast(0)
    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
