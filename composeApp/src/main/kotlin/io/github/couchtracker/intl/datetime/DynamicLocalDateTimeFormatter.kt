package io.github.couchtracker.intl.datetime

import android.content.Context
import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.MeasureFormat.FormatWidth
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.R
import io.github.couchtracker.intl.formatDateTimeSkeleton
import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.combine
import io.github.couchtracker.utils.flatMap
import io.github.couchtracker.utils.withNextTickAtMost
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

private val RELATIVE_DATE_THRESHOLD = 10.days
private val DURATION_THRESHOLD = 1.days

/**
 * Class to format a [LocalDateTime] dynamically based on its distance from now.
 *
 * If the distance is far enough (> 10 days), the date is simply formatted in an absolute way using the given [absoluteDateSkeletons] and
 * [timeSkeleton] (see [formatDateTimeSkeleton]). Examples:
 * - `Jan 25, 2026, 3:50 PM`
 * - `01/25/2026, 12:00 AM`
 *
 * Otherwise, if the difference is less than 10 days, the [LocalDateTime] is formatted using [timeSkeleton] and [relativeDateStyle] (see
 * [RelativeDateAbsoluteTimeFormatter]). Examples:
 * - `today at 10:30`
 * - `this Monday at 6pm`
 * - `5 days ago at 4:21`
 *
 * Finally, if the difference is also less than 24 hours, a duration format using [relativeDurationFormatWidth],
 * [relativeDurationOmitZeros], [relativeDurationMaxUnits], and a min unit derived from [timeSkeleton] is appended in parentheses (see
 * [RelativeDurationFormatter]). Examples:
 * - `today at 10:30 (in 3 hours, 5 minutes)`
 * - `yesterday at 9:00 PM (15h 5m ago)`
 *
 * The [context] is needed to retrieve the localization strings for "in x", "x ago", and one for parenthesizing. For this reason, [locale]
 * should always match the locale of the given [context].
 */
class DynamicLocalDateTimeFormatter(
    private val context: Context,
    private val locale: ULocale,
    private val timeSkeleton: TimeSkeleton = TimeSkeleton.MINUTES,
    private val timeZoneSkeleton: TimezoneSkeleton = TimezoneSkeleton.TIMEZONE_ID,
    relativeDateStyle: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    private val absoluteDateSkeletons: List<DateSkeleton> = Skeletons.MEDIUM_DATE,
    relativeDurationFormatWidth: FormatWidth = FormatWidth.NARROW,
    relativeDurationOmitZeros: Boolean = true,
    relativeDurationMaxUnits: Int = 2,
) {

    private val relativeDateAbsoluteTimeFormatter = RelativeDateAbsoluteTimeFormatter(
        locale = locale,
        timeSkeleton = timeSkeleton,
        timeZoneSkeleton = timeZoneSkeleton,
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

    fun format(dateTime: MaybeZoned<LocalDateTime>, now: Zoned<Instant>): TickingValue<String> {
        val tz = dateTime.timeZone ?: now.timeZone
        val instant = dateTime.value.toInstant(tz)
        val diff = instant - now.value

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
            val relAbsFormat = relativeDateAbsoluteTimeFormatter.format(dateTime, now)
            return chooseThreshold(
                threshold = DURATION_THRESHOLD,
                withinThreshold = {
                    val relDurationFormat = relativeDurationFormatter.format(diff).flatMap {
                        if (diff.isNegative()) {
                            TickingValue(
                                value = context.getString(R.string.duration_x_ago, it),
                                nextTick = null,
                            )
                        } else {
                            TickingValue(
                                value = context.getString(R.string.duration_in_x, it),
                                nextTick = diff + 1.nanoseconds,
                            )
                        }
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
                val skeletons = listOfNotNull(
                    timeSkeleton,
                    timeZoneSkeleton.takeIf { dateTime.timeZone != null && dateTime.timeZone != now.timeZone },
                )
                TickingValue(
                    value = formatDateTimeSkeleton(
                        instant = instant,
                        timeZone = tz,
                        skeleton = (absoluteDateSkeletons + skeletons).sum(),
                        locale = locale,
                    ),
                    nextTick = null,
                )
            },
        )
    }
}
