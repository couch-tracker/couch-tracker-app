package io.github.couchtracker.intl.datetime

import io.github.couchtracker.db.user.model.partialtime.PartialDateTime

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
 * A specialization of [Skeleton], which has a type [L] that must be a subtype of [PartialDateTime.Local].
 *
 * This is helpful to know which component is required for this skeleton to have a meaning.
 */
interface LocalSkeleton<in L : PartialDateTime.Local> : Skeleton

/**
 * See [Unicode docs](https://www.unicode.org/reports/tr35/tr35-dates.html#dfst-year).
 */
enum class YearSkeleton(override val value: String) : LocalSkeleton<PartialDateTime.Local.WithYear> {

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
enum class MonthSkeleton(override val value: String) : LocalSkeleton<PartialDateTime.Local.WithMonth> {
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
enum class DayOfMonthSkeleton(override val value: String) : LocalSkeleton<PartialDateTime.Local.WithDay> {
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
enum class DayOfWeekSkeleton(override val value: String) : LocalSkeleton<PartialDateTime.Local.WithDay> {
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
enum class TimeSkeleton(override val value: String) : LocalSkeleton<PartialDateTime.Local.WithTime> {
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

private class PlusSkeleton(skeletons: Collection<Skeleton>) : Skeleton {
    override val value = skeletons.joinToString(separator = " ") { it.value }
}

/**
 * Creates a new [Skeleton] all concatenating all items in this collection.
 */
fun Collection<Skeleton>.sum(): Skeleton = PlusSkeleton(this)

private typealias DateSkeleton = LocalSkeleton<PartialDateTime.Local.WithDate>

object Skeletons {
    val NUMERIC_DATE = listOf<DateSkeleton>(YearSkeleton.NUMERIC, MonthSkeleton.NUMERIC, DayOfMonthSkeleton.NUMERIC)
    val MEDIUM_DATE = listOf<DateSkeleton>(YearSkeleton.NUMERIC, MonthSkeleton.ABBREVIATED, DayOfMonthSkeleton.NUMERIC)
    val LONG_DATE = listOf<DateSkeleton>(YearSkeleton.NUMERIC, MonthSkeleton.WIDE, DayOfMonthSkeleton.NUMERIC)
    val FULL_DATE = listOf<DateSkeleton>(YearSkeleton.NUMERIC, MonthSkeleton.WIDE, DayOfMonthSkeleton.NUMERIC, DayOfWeekSkeleton.WIDE)
}
