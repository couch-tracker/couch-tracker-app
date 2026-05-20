package io.github.couchtracker.intl.datetime

import kotlin.collections.setOfNotNull

/**
 * Represents a Unicode CLDR Skeleton.
 *
 * The skeleton itself is not a pattern that is formatted directly, but the most appropriate pattern will be selected given the information
 * provided by the skeleton.
 */
interface Skeleton {
    /**
     * The value of the skeleton, e.g. `y MM` for a string containing the year number and the month number
     */
    val value: String
}

/**
 * See [Unicode docs](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-year).
 */
enum class YearSkeleton(override val value: String) : Skeleton {

    /**
     * Two low-order digits of the year, e.g. `95` for 1995
     */
    SHORT("yy"),

    /**
     * Numeric, non-padded year number, e.g. `201`, `2017`
     */
    NUMERIC("y"),
}

/**
 * See [Unicode docs](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-month).
 */
enum class MonthSkeleton(override val value: String) : Skeleton {
    /**
     * Numeric, zero-padded month, e.g. `02` for February
     */
    NUMERIC("MM"),

    /**
     * Numeric, non-padded month, e.g. `2` for February
     */
    NUMERIC_NON_PADDED("M"),

    /**
     * Narrow month name, e.g. `F` for February
     */
    NARROW("MMMMM"),

    /**
     * Abbreviated month name, e.g. `Feb` for February
     */
    ABBREVIATED("MMM"),

    /**
     * Full moth name, e.g. `February`
     */
    WIDE("MMMM"),
}

/**
 * See [Unicode docs](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-day).
 */
enum class DayOfMonthSkeleton(override val value: String) : Skeleton {
    /**
     * Numeric, zero-padded day-of-month, e.g. `09`
     */
    NUMERIC("dd"),

    /**
     * Numeric, non-padded day-of-month, e.g. `9`
     */
    NUMERIC_NON_PADDED("d"),
}

/**
 * See [Unicode docs](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-weekday).
 */
enum class DayOfWeekSkeleton(override val value: String) : Skeleton {
    /**
     * Narrow day-of-week name, e.g. `T` for Tuesday
     */
    NARROW("EEEEE"),

    /**
     * Short day-of-week name, e.g. `Tu` for Tuesday
     */
    SHORT("EEEEEE"),

    /**
     * Abbreviated day-of-week name, e.g. `Tue` for Tuesday
     */
    ABBREVIATED("E"),

    /**
     * Full day-of-week name, e.g. `Tuesday`
     */
    WIDE("EEEE"),
}

/**
 * See Unicode docs:
 *  - [hour](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-minute)
 *  - [minute](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-minute)
 *  - [second](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-second)
 */
enum class TimeSkeleton(override val value: String) : Skeleton {
    /**
     * Time with hours and minutes, zero-padded. 12/24 hours is locale-dependent
     */
    MINUTES("jj mm"),

    /**
     * Time with hours, minutes and seconds, zero-padded. 12/24 hours is locale-dependent
     */
    SECONDS("jj mm ss"),
}

/**
 * See [Unicode docs](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-zone).
 */
enum class TimezoneSkeleton(override val value: String) : Skeleton {
    /**
     * The ISO8601 extended format with hours and minutes fields, e.g. `+01:00`
     */
    OFFSET("xxx"),

    /**
     * The long timezone ID, e.g. `Europe/Dublin`
     */
    TIMEZONE_ID("VV"),

    /**
     * The long specific non-location format, e.g. `Irish standard time`
     */
    SPECIFIC_NON_LOCATION("zzzz"),
}

interface SkeletonGroup : Skeleton {
    val skeletons: Collection<Skeleton>

    override val value: String
        get() = skeletons.joinToString(separator = " ") { it.value }
}

data class DateSkeleton(
    val year: YearSkeleton? = null,
    val month: MonthSkeleton? = null,
    val dayOfMonth: DayOfMonthSkeleton? = null,
    val dayOfWeekSkeleton: DayOfWeekSkeleton? = null,
) : SkeletonGroup {
    init {
        require(year != null || month != null || dayOfMonth != null || dayOfWeekSkeleton != null) { "At least one skeleton must be set" }
    }

    override val skeletons: Set<Skeleton>
        get() = setOfNotNull(year, month, dayOfMonth, dayOfWeekSkeleton)
}

data class DateTimeSkeleton(
    val date: DateSkeleton?,
    val time: TimeSkeleton?,
) : SkeletonGroup {

    override val skeletons: Set<Skeleton>
        get() = date?.skeletons.orEmpty() + setOfNotNull(time)

    constructor(
        year: YearSkeleton? = null,
        month: MonthSkeleton? = null,
        dayOfMonth: DayOfMonthSkeleton? = null,
        dayOfWeek: DayOfWeekSkeleton? = null,
        time: TimeSkeleton? = null,
    ) : this(DateSkeleton(year, month, dayOfMonth, dayOfWeek), time)
}

private class PlusSkeleton(override val skeletons: Collection<Skeleton>) : SkeletonGroup {
    override val value = skeletons.joinToString(separator = " ") { it.value }
}

/**
 * Creates a new [Skeleton] all concatenating all items in this collection.
 */
fun Collection<Skeleton>.sum(): Skeleton = PlusSkeleton(this)

object Skeletons {
    val NUMERIC_DATE = DateSkeleton(YearSkeleton.NUMERIC, MonthSkeleton.NUMERIC, DayOfMonthSkeleton.NUMERIC)
    val MEDIUM_DATE = DateSkeleton(YearSkeleton.NUMERIC, MonthSkeleton.ABBREVIATED, DayOfMonthSkeleton.NUMERIC)
    val LONG_DATE = DateSkeleton(YearSkeleton.NUMERIC, MonthSkeleton.WIDE, DayOfMonthSkeleton.NUMERIC, DayOfWeekSkeleton.ABBREVIATED)
    val FULL_DATE = DateSkeleton(YearSkeleton.NUMERIC, MonthSkeleton.WIDE, DayOfMonthSkeleton.NUMERIC, DayOfWeekSkeleton.WIDE)
}
