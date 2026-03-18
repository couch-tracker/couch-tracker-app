package io.github.couchtracker.intl.datetime

import com.ibm.icu.util.ULocale
import io.github.couchtracker.intl.formatDateTimeSkeleton
import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.Zoned
import kotlinx.datetime.TimeZone
import kotlin.time.Instant

abstract class AbstractDynamicLocalFormatter<T, DIFF>(
    private val locale: ULocale,
    private val timeZoneSkeleton: TimezoneSkeleton,
) {
    protected data class FormatContext<T, DIFF>(
        val localValue: MaybeZoned<T>,
        val now: Zoned<Instant>,
        val timeZone: TimeZone,
        val valueInstant: Instant,
        val diff: DIFF,
    )

    protected abstract val relativeThreshold: DIFF
    protected abstract val absoluteSkeleton: Skeleton
    protected abstract fun T.localToInstant(timeZone: TimeZone): Instant
    protected abstract fun calculateDiff(valueInstant: Instant, now: Zoned<Instant>, timeZone: TimeZone): DIFF

    protected abstract fun FormatContext<T, DIFF>.chooseThreshold(
        threshold: DIFF,
        withinThreshold: () -> TickingValue<String>,
        outsideThreshold: () -> TickingValue<String>,
    ): TickingValue<String>

    private fun FormatContext<T, DIFF>.formatAbsolute(): TickingValue<String> {
        val tzSkeleton = timeZoneSkeleton.takeIf { localValue.timeZone != null && localValue.timeZone != now.timeZone }
        return TickingValue(
            value = formatDateTimeSkeleton(
                instant = valueInstant,
                timeZone = timeZone,
                skeleton = listOfNotNull(absoluteSkeleton, tzSkeleton).sum(),
                locale = locale,
            ),
            nextTick = null,
        )
    }

    protected abstract fun FormatContext<T, DIFF>.formatRelative(): TickingValue<String>

    protected fun formatLocalValue(localValue: MaybeZoned<T>, now: Zoned<Instant>): TickingValue<String> {
        val timeZone = localValue.timeZone ?: now.timeZone
        val valueInstant = localValue.value.localToInstant(timeZone)
        val context = FormatContext(
            localValue = localValue,
            now = now,
            timeZone = timeZone,
            valueInstant = valueInstant,
            diff = calculateDiff(valueInstant, now, timeZone),
        )

        return context.chooseThreshold(
            threshold = relativeThreshold,
            withinThreshold = { context.formatRelative() },
            outsideThreshold = { context.formatAbsolute() },
        )
    }
}
