package io.github.couchtracker.intl.datetime

import android.content.Context
import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.MeasureFormat.FormatWidth
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.R
import io.github.couchtracker.intl.formatDateTimeSkeleton
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.combine
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.withNextTickAtMost
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

private val RELATIVE_DATE_THRESHOLD = 10.days
private val DURATION_THRESHOLD = 1.days

class DynamicLocalDateTimeFormatter(
    private val context: Context,
    private val locale: ULocale,
    private val timeSkeleton: TimeSkeleton = TimeSkeleton.MINUTES,
    relativeDateStyle: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    private val absoluteDateSkeletons: List<DateSkeleton> = Skeletons.MEDIUM_DATE,
    relativeDurationFormatWidth: FormatWidth = FormatWidth.NARROW,
    relativeDurationOmitZeros: Boolean = true,
    relativeDurationMaxUnits: Int = 2,
) {

    private val relativeDateAbsoluteTimeFormatter = RelativeDateAbsoluteTimeFormatter(
        locale = locale,
        timeSkeleton = timeSkeleton,
        relativeDateStyle = relativeDateStyle,
        dateFormatStyle = DateFormat.MEDIUM,
    )
    private val relativeDurationFormatter = RelativeDurationFormatter(
        locale = locale,
        formatWidth = relativeDurationFormatWidth,
        omitZeros = relativeDurationOmitZeros,
        minUnit = when (timeSkeleton) {
            TimeSkeleton.MINUTES -> DurationUnit.MINUTES
            TimeSkeleton.SECONDS -> DurationUnit.SECONDS
        },
        maxUnits = relativeDurationMaxUnits,
    )

    fun format(dateTime: LocalDateTime, now: Instant, tz: TimeZone): TickingValue<String> {
        val instant = dateTime.toInstant(tz)
        val diff = instant - now

        fun chooseThreshold(
            threshold: Duration,
            withinThreshold: () -> TickingValue<String>,
            outsideThreshold: () -> TickingValue<String>,
        ): TickingValue<String> {
            return if (diff.absoluteValue < threshold) {
                val willGoOutsideThresholdOrSwitchSignIn = (if (diff.isNegative()) threshold + diff else diff + 1.nanoseconds)
                withinThreshold().withNextTickAtMost(willGoOutsideThresholdOrSwitchSignIn)
            } else {
                val willGoWithinThresholdIn = if (diff.isNegative()) null else diff - threshold + 1.nanoseconds
                outsideThreshold().withNextTickAtMost(willGoWithinThresholdIn)
            }
        }

        fun formatRelative(): TickingValue<String> {
            val relAbsFormat = relativeDateAbsoluteTimeFormatter.format(dateTime, now, tz)
            return chooseThreshold(
                threshold = DURATION_THRESHOLD,
                withinThreshold = {
                    val relDurationFormat = relativeDurationFormatter.format(diff).map {
                        context.getString(if (diff.isNegative()) R.string.duration_x_ago else R.string.duration_in_x, it)
                    }
                    relAbsFormat.combine(relDurationFormat) { relAbsFormat, relDurationFormat ->
                        context.getString(R.string.parenthesize, relAbsFormat, relDurationFormat)
                    }
                },
                outsideThreshold = { relAbsFormat },
            )
        }

        return chooseThreshold(
            threshold = RELATIVE_DATE_THRESHOLD,
            withinThreshold = ::formatRelative,
            outsideThreshold = {
                TickingValue(
                    value = formatDateTimeSkeleton(
                        instant = instant,
                        timeZone = tz,
                        skeleton = (absoluteDateSkeletons + timeSkeleton).sum(),
                        locale = locale,
                    ),
                    nextTick = null,
                )
            },
        )
    }
}
