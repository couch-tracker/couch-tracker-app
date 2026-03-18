package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import com.ibm.icu.text.RelativeDateTimeFormatter.Direction
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.Zoned
import kotlinx.datetime.TimeZone
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Instant

abstract class AbstractRelativeLocalFormatter<T>(
    locale: ULocale,
    style: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    capitalizationContext: DisplayContext = DisplayContext.CAPITALIZATION_NONE,
) {

    protected val relativeDateTimeFormatter: RelativeDateTimeFormatter =
        RelativeDateTimeFormatter.getInstance(locale, null, style, capitalizationContext)

    protected data class FormatContext<T>(
        val localValue: T,
        val now: Zoned<Instant>,
        val nowLocal: T,
        val diff: Int,
        val nextUnitTick: Duration,
    )

    protected abstract val absoluteUnit: AbsoluteUnit
    protected abstract val relativeUnit: RelativeUnit
    protected abstract fun Zoned<Instant>.toLocalPart(): T
    protected abstract fun T.unitsUntil(other: T): Int
    protected abstract fun startOfNextUnit(value: T, timeZone: TimeZone): Instant

    protected open fun FormatContext<T>.formats(): Sequence<TickingValue<String>> = sequence {
        formatDirection()?.let { yield(it) }
        yield(formatRelativeUnit())
    }

    @Suppress("MagicNumber")
    protected fun FormatContext<T>.formatDirection(): TickingValue<String>? {
        val direction = when (diff) {
            -2 -> Direction.LAST_2
            -1 -> Direction.LAST
            0 -> Direction.THIS
            +1 -> Direction.NEXT
            +2 -> Direction.NEXT_2
            else -> null
        }
        return direction?.let {
            // This returns null if the locale doesn't have a way to say this
            relativeDateTimeFormatter.format(it, absoluteUnit)
        }?.let {
            TickingValue(it, nextTick = nextUnitTick)
        }
    }

    protected fun FormatContext<T>.formatRelativeUnit(): TickingValue<String> {
        return TickingValue(
            value = relativeDateTimeFormatter.format(
                diff.absoluteValue.toDouble(),
                if (diff < 0) Direction.LAST else Direction.NEXT,
                relativeUnit,
            ),
            nextTick = nextUnitTick,
        )
    }

    protected fun formatLocalValue(localValue: T, now: Zoned<Instant>): TickingValue<String> {
        val nowLocal = now.toLocalPart()
        val context = FormatContext(
            localValue = localValue,
            now = now,
            nowLocal = nowLocal,
            diff = nowLocal.unitsUntil(localValue),
            nextUnitTick = startOfNextUnit(nowLocal, now.timeZone) - now.value,
        )

        return context.formats().first()
    }
}
