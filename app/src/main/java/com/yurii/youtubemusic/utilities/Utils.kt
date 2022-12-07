package com.yurii.youtubemusic.utilities

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import com.yurii.youtubemusic.R
import org.threeten.bp.Duration
import java.lang.IllegalStateException
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit

fun parseDurationToHumanView(text: String): String {
    val millis = Duration.parse(text).toMillis()
    return parseDurationToHumanView(millis)
}

fun parseDurationToHumanView(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % TimeUnit.HOURS.toMinutes(1)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % TimeUnit.MINUTES.toSeconds(1)

    val secondsString = if (seconds < 10) "0$seconds" else seconds.toString()

    return if (hours > 0) "$hours:$minutes:$secondsString" else "$minutes:$secondsString"
}


fun bigIntegerToShortCutSuffix(value: BigInteger): String {
    return longToShortCutSuffix(value.toLong())
}

fun longToShortCutSuffix(value: Long): String {
    val suffixes: TreeMap<Long, Char> = TreeMap()
    suffixes[1_000L] = 'K'
    suffixes[1_000_000L] = 'M'
    suffixes[1_000_000_000L] = 'B'
    suffixes[1_000_000_000_000L] = 'T'
    suffixes[1_000_000_000_000_000L] = 'P'

    if (value == java.lang.Long.MIN_VALUE) return longToShortCutSuffix(java.lang.Long.MIN_VALUE + 1)
    if (value < 0) return "-${longToShortCutSuffix(-value)}"
    if (value < 1000) return value.toString()

    val e = suffixes.floorEntry(value)
    val divideBy: Long = e!!.key
    val suffix: Char = e.value

    val truncated: Long = value / (divideBy / 10)
    val hasDecimal: Boolean = truncated < 100 && truncated / 10.0 != (truncated / 10).toDouble()
    return if (hasDecimal) "${truncated / 10.0}$suffix" else "${truncated / 10}$suffix"
}

fun calculateLikeBarValue(likeCount: BigInteger, disLikeCount: BigInteger): Int {
    val sum = likeCount.plus(disLikeCount)
    return if (sum.compareTo(BigInteger.ZERO) == 0) 50 else likeCount.multiply(BigInteger("100")).divide(sum).toInt()
}

fun createFromPathOrReturnMock(context: Context, path: String?): Drawable {
    return Drawable.createFromPath(path) ?: context.getDrawable(R.drawable.ic_thumbnail_mock)!!
}

fun <T : Parcelable> Bundle.requireParcelable(key: String): T {
    return this.getParcelable(key) ?: throw IllegalStateException("This bundle argument is required: $key")
}


