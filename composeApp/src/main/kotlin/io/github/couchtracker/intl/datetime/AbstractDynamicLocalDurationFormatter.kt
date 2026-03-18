package io.github.couchtracker.intl.datetime

import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.withNextTickAtMost
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

abstract class AbstractDynamicLocalDurationFormatter<T>(
    locale: ULocale,
    timeZoneSkeleton: TimezoneSkeleton,
) : AbstractDynamicLocalFormatter<T, Duration>(
    locale = locale,
    timeZoneSkeleton = timeZoneSkeleton,
) {
    final override fun FormatContext<T, Duration>.chooseThreshold(
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
}
