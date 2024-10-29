package io.github.couchtracker.db.user.model.partialtime

import io.github.couchtracker.db.user.model.partialtime.PartialDateTime.Local
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime.Local.Date
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime.Local.DateTime
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime.Local.Year
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime.Local.YearMonth
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime.Zoned
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.asTimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.format.format
import kotlinx.datetime.toInstant

/**
 * Represents a date/time value that might not have all the components present.
 *
 * This class is needed to store a watch date of a movie/episode, as the user might not know/remember all details of when something was
 * watched.
 *
 * **Not all combinations of missing data are supported**. In ascending order of precision, when a component is unknown, all components
 * after it must be as well. Time is considered a single indivisible component.
 *
 * This is done to have a cleaner UI, as supporting true partial data like "July 15 of unknown year, at unknown hour at the 45-minute mark"
 * would make the UI needlessly more cumbersome for little benefit, as users are likely to remember all components up to a certain point
 * anyway.
 *
 * Supported types are:
 * - [Local.Year] just a year, e.g. 2024
 * - [Local.YearMonth] year and month, e.g. July 2024
 * - [Local.Date] date, e.g. July 12th, 2024
 * - [Local.DateTime] date and time, e.g. July 12th, 2024 at 21:00:00
 * - [Zoned] composes any [Local] value with a [TimeZone]
 *
 * Since sorting [PartialDateTime]s is a difficult task, implementing [Comparable] is not possible: the sorting algorithm needs to consider
 * multiple factors, deciding whether A <=> B is undecidable in isolation. See [PartialDateTime.sort] for more info.
 * However, both [Local] and [Zoned] implement [Comparable].
 */
sealed interface PartialDateTime {

    /**
     * Returns the local part of this instance. For [Local] instance, this is the same object on which this is called
     */
    val local: Local

    /**
     * Serializes this object to a string.
     *
     * @see PartialDateTime.parse for parsing the string back to a [PartialDateTime]
     */
    fun serialize(): String

    /**
     * Returns in which [PartialDateTimeGroup] the partial date time belongs to. There is also an extension function on the nullable type
     * that returns [PartialDateTimeGroup.Unknown] in that case.
     */
    fun group(): PartialDateTimeGroup

    /**
     * Interface for companion object that each subclass should implement that defines a [parse] method.
     */
    interface ICompanion<out P : PartialDateTime> {

        /**
         * Parses the given [input] into a [P].
         * @throws IllegalArgumentException if the input is invalid
         */
        fun parse(input: String): P
    }

    /**
     * Specialization of [ICompanion] that handles parsing with a given [DateTimeFormat] of [DateTimeComponents].
     */
    @Suppress("VariableNaming")
    abstract class ICompanionWithFormat<out P : PartialDateTime> : ICompanion<P> {
        abstract val FORMAT: DateTimeFormat<DateTimeComponents>

        /**
         * Creates a [P] from the given [components].
         * @throws IllegalArgumentException if a required component is present or is not valid
         */
        abstract fun fromComponents(components: DateTimeComponents): P

        override fun parse(input: String): P = fromComponents(FORMAT.parse(input))
    }

    /**
     * Specialization of [ICompanion] that handles parsing with a given list of parser functions.
     *
     * The result of first parser that doesn't throw [IllegalArgumentException] is propagated to the caller.
     */
    @Suppress("VariableNaming")
    abstract class ICompanionWithParsers<out P : PartialDateTime> : ICompanion<P> {
        abstract val PARSERS: List<(String) -> P>

        override fun parse(input: String): P {
            val exceptions = mutableListOf<IllegalArgumentException>()
            for (parser in PARSERS) {
                try {
                    return parser(input)
                } catch (e: IllegalArgumentException) {
                    exceptions.add(e)
                }
            }
            val messages = exceptions.mapNotNull { it.message }.joinToString(", ")
            throw IllegalArgumentException("Invalid input: $input. Couldn't be parsed by any format: $messages")
        }
    }

    /**
     * Represents a date/time that has no time zone information.
     *
     * Implements [Comparable]. Unknown information is considered always < than more precise values.
     *
     * For example: `2024` < `2024-01` < `2024-01-01` < `2024-01-01T00:00:00`
     *
     * @see PartialDateTime
     */
    sealed interface Local : PartialDateTime, Comparable<Local> {

        sealed interface WithYear : Local {
            val year: Int
        }

        sealed interface WithMonth : Local {
            val month: Month
        }

        sealed interface WithYearMonth : WithYear, WithMonth

        sealed interface WithDay : Local {
            val dayOfWeek: DayOfWeek
            val dayOfMonth: Int
            val dayOfYear: Int
        }

        sealed interface WithDate : WithYearMonth, WithDay {
            val date: LocalDate

            override val year get() = date.year
            override val month get() = date.month
            override val dayOfWeek get() = date.dayOfWeek
            override val dayOfMonth get() = date.dayOfMonth
            override val dayOfYear get() = date.dayOfYear
        }

        sealed interface WithTime : Local {
            val time: LocalTime
        }

        sealed interface WithDateTime : WithDate, WithTime {
            val dateTime: LocalDateTime

            override val date get() = dateTime.date
            override val time get() = dateTime.time
        }

        override val local get() = this

        /**
         * Converts this to an [Instant] using the provided time zone [zone].
         * Partial information is filled in with a default value.
         */
        fun toInstant(zone: TimeZone): Instant

        /**
         * Represents a whole year, e.g. 2024
         */
        data class Year(override val year: Int) : Local, WithYear {
            override fun group() = PartialDateTimeGroup.Year(this)
            override fun serialize() = FORMAT.format {
                year = this@Year.year
            }

            override fun toInstant(zone: TimeZone) = LocalDate(year, Month.JANUARY, dayOfMonth = 1).atStartOfDayIn(zone)

            companion object : ICompanionWithFormat<Year>() {
                override val FORMAT = DateTimeComponents.Format { year(Padding.ZERO) }

                override fun fromComponents(components: DateTimeComponents) = Year(year = requireNotNull(components.year))
            }
        }

        /**
         * Represents a month in a given year, e.g. July 2024
         */
        data class YearMonth(override val year: Int, override val month: Month) : Local, WithYearMonth {
            override fun group() = PartialDateTimeGroup.YearMonth(this)
            override fun serialize() = FORMAT.format {
                year = this@YearMonth.year
                month = this@YearMonth.month
            }

            override fun toInstant(zone: TimeZone) = LocalDate(year, month, dayOfMonth = 1).atStartOfDayIn(zone)

            companion object : ICompanionWithFormat<YearMonth>() {
                override val FORMAT = DateTimeComponents.Format {
                    year(Padding.ZERO)
                    char('-')
                    monthNumber(Padding.ZERO)
                }

                override fun fromComponents(components: DateTimeComponents) = YearMonth(
                    year = requireNotNull(components.year),
                    month = requireNotNull(components.month),
                )
            }
        }

        /**
         * Represents a full date with no time component, e.g. 15th of July 2024
         */
        data class Date(override val date: LocalDate) : Local, WithDate {
            override fun group() = PartialDateTimeGroup.YearMonth(YearMonth(date.year, date.month))
            override fun serialize() = FORMAT.format { setDate(date) }

            override fun toInstant(zone: TimeZone) = date.atStartOfDayIn(zone)

            companion object : ICompanionWithFormat<Date>() {
                override val FORMAT = DateTimeComponents.Format { date(LocalDate.Formats.ISO) }

                override fun fromComponents(components: DateTimeComponents) = Date(components.toLocalDate())
            }
        }

        /**
         * Represents a full date and time, e.g. 15:00 15th of July 2024
         */
        data class DateTime(override val dateTime: LocalDateTime) : Local, WithDateTime {
            override fun group() = PartialDateTimeGroup.YearMonth(YearMonth(dateTime.year, dateTime.month))
            override fun serialize() = FORMAT.format { setDateTime(dateTime) }

            override fun toInstant(zone: TimeZone) = dateTime.toInstant(zone)

            companion object : ICompanionWithFormat<DateTime>() {
                override val FORMAT = DateTimeComponents.Format { dateTime(LocalDateTime.Formats.ISO) }

                override fun fromComponents(components: DateTimeComponents) = DateTime(components.toLocalDateTime())
            }
        }

        /**
         * Returns a [Zoned] with this instance as its local part and [timeZone] for the timezone.
         */
        fun atZone(timeZone: TimeZone) = Zoned(local = this, zone = timeZone)

        override fun compareTo(other: Local) = LOCAL_COMPARATOR.compare(this, other)

        companion object : ICompanionWithParsers<Local>() {
            override val PARSERS: List<(String) -> Local> by lazy { listOf(Year::parse, YearMonth::parse, Date::parse, DateTime::parse) }

            private val FROM_COMPONENTS: List<(DateTimeComponents) -> Local> by lazy {
                listOf(
                    DateTime::fromComponents,
                    Date::fromComponents,
                    YearMonth::fromComponents,
                    Year::fromComponents,
                )
            }

            fun fromComponents(components: DateTimeComponents): Local {
                return FROM_COMPONENTS.firstNotNullOf { fromComponents ->
                    try {
                        fromComponents(components)
                    } catch (ignored: IllegalArgumentException) {
                        null
                    }
                }
            }
        }
    }

    /**
     * Enriches a [Local] time with a [TimeZone]. This way any local time can have a timezone attached.
     *
     * This is useful as in most cases users will know which time zone they have watched a movie in, but might not remember the full details
     * like the day or the time.
     *
     * Implements [Comparable] with the following rules:
     * 1. The local year and month are always sorted first, without taking the timezone into consideration, with the same rules as [Local];
     * 2. After that, [toInstant] is used to compare instances;
     * 3. When instants are the same, less precise objects are put first, in the same vein as [Local].
     *
     * Example: `2024-18:00` < `2024-01-10:00` < `2024-01-01T00:00:00+05:00` < `2024-01-01+00:00`
     */
    data class Zoned(
        override val local: Local,
        val zone: TimeZone,
    ) : PartialDateTime, Comparable<Zoned> {

        override fun group() = local.group()

        override fun serialize(): String {
            val zonePart = when (zone) {
                is FixedOffsetTimeZone -> zone.offset.format(OFFSET_FORMAT)
                else -> IANA_FORMAT.format { timeZoneId = zone.id }
            }
            return local.serialize() + zonePart
        }

        /**
         * Converts this instance's [local] to an [Instant] using [zone] as a time zone
         *
         * @see Local.toInstant
         */
        fun toInstant() = local.toInstant(zone)

        override fun compareTo(other: Zoned) = ZONED_COMPARATOR.compare(this, other)

        companion object : ICompanion<Zoned> {

            private val OFFSET_FORMAT = UtcOffset.Formats.ISO
            private val IANA_FORMAT = DateTimeComponents.Format {
                char('[')
                timeZoneId()
                char(']')
            }

            // Can be used for parsing only
            private val PARSE_FORMAT = DateTimeComponents.Format {
                // Any type of Local value
                alternativeParsing(
                    { dateTimeComponents(Date.FORMAT) },
                    { dateTimeComponents(YearMonth.FORMAT) },
                    { dateTimeComponents(Year.FORMAT) },
                ) { dateTimeComponents(DateTime.FORMAT) }

                // Either offset or IANA timezone ID
                alternativeParsing({ offset(OFFSET_FORMAT) }) {
                    dateTimeComponents(IANA_FORMAT)
                }
            }

            override fun parse(input: String): Zoned {
                val components = PARSE_FORMAT.parse(input)
                return fromComponents(components)
            }

            fun fromComponents(components: DateTimeComponents): Zoned {
                val local = Local.fromComponents(components)
                val timeZone = when (val timeZoneId = components.timeZoneId) {
                    null -> components.toUtcOffset().asTimeZone()
                    else -> TimeZone.of(timeZoneId)
                }
                return Zoned(local, timeZone)
            }
        }
    }

    companion object : ICompanionWithParsers<PartialDateTime>() {

        override val PARSERS: List<(String) -> PartialDateTime> by lazy { Local.PARSERS + listOf(Zoned::parse) }

        /**
         * Sorts [PartialDateTime] only by their local year and month.
         */
        private val LOCAL_YEAR_MONTH_COMPARATOR: Comparator<PartialDateTime> = compareBy<PartialDateTime> {
            when (val local = it.local) {
                is Year -> local.year
                is YearMonth -> local.year
                is Date -> local.date.year
                is DateTime -> local.dateTime.year
            }
        }.thenBy {
            when (val local = it.local) {
                is Year -> 0
                is YearMonth -> local.month.value
                is Date -> local.date.month.value
                is DateTime -> local.dateTime.month.value
            }
        }

        /**
         * Sorts [PartialDateTime] only by their local part. At parity of local part, [Zoned] values are put last.
         */
        private val LOCAL_COMPARATOR: Comparator<PartialDateTime> = LOCAL_YEAR_MONTH_COMPARATOR.thenBy {
            when (val local = it.local) {
                is Year, is YearMonth -> Int.MIN_VALUE
                is Date -> local.date.toEpochDays()
                is DateTime -> local.dateTime.date.toEpochDays()
            }
        }.thenBy {
            when (val local = it.local) {
                is Year, is YearMonth, is Date -> Instant.DISTANT_PAST
                is DateTime -> local.dateTime.toInstant(UtcOffset.ZERO)
            }
        }.thenBy {
            when (it) {
                is Local -> 0
                is Zoned -> 1
            }
        }

        private val ZONED_COMPARATOR: Comparator<Zoned> = compareBy<Zoned, Local>(LOCAL_YEAR_MONTH_COMPARATOR) { it.local }
            .thenBy {
                when (it.local) {
                    is Year, is YearMonth -> 0
                    is Date, is DateTime -> 1
                }
            }
            .thenBy { it.toInstant() }
            .thenBy {
                when (it.local) {
                    is Year -> 0
                    is YearMonth -> 1
                    is Date -> 2
                    is DateTime -> 3
                }
            }

        /**
         * Sorts the given [items] based on their [PartialDateTime], retrieved with the [getPartialDateTime] lambda.
         * Upon identical [PartialDateTime], [additionalComparator] is used on the items [T].
         *
         * The algorithm works like this:
         * - First it splits [Local] and [Zoned] values into two different lists, and sorts them according to their comparator;
         * - Then, the two lists are merged together, sorted according to their local part only.
         *
         * This ensures that the [Local] and [Zoned] are always sorted among themselves.
         *
         * @see PartialDateTime.sortAndGroup
         */
        fun <T> sort(
            items: Iterable<T>,
            getPartialDateTime: T.() -> PartialDateTime,
            additionalComparator: Comparator<T> = compareBy { 0 },
        ): List<T> {
            data class ItemWithDate(val item: T, val date: PartialDateTime)

            val itemsWithDates = items.map { ItemWithDate(item = it, date = it.getPartialDateTime()) }

            val (locals, zoned) = itemsWithDates.partition { it.date is Local }

            val sortedLocals = locals
                .sortedWith(compareBy<ItemWithDate, Local>(LOCAL_COMPARATOR) { it.date as Local }.thenBy(additionalComparator) { it.item })
                .toCollection(ArrayDeque(locals.size))

            val sortedZoned = zoned
                .sortedWith(compareBy<ItemWithDate, Zoned>(ZONED_COMPARATOR) { it.date as Zoned }.thenBy(additionalComparator) { it.item })
                .toCollection(ArrayDeque(zoned.size))

            return buildList {
                fun pushLocal() = add(sortedLocals.removeFirst().item)
                fun pushZoned() = add(sortedZoned.removeFirst().item)

                while (sortedLocals.isNotEmpty() || sortedZoned.isNotEmpty()) {
                    if (sortedLocals.isNotEmpty() && sortedZoned.isNotEmpty()) {
                        if (LOCAL_COMPARATOR.compare(sortedLocals.first().date, sortedZoned.first().date) < 0) {
                            pushLocal()
                        } else {
                            pushZoned()
                        }
                    } else if (sortedLocals.isNotEmpty()) {
                        pushLocal()
                    } else {
                        pushZoned()
                    }
                }
            }
        }

        /**
         * Sorts the given [items], by their [PartialDateTime] and groups them according to the [PartialDateTime]'s [PartialDateTime.group].
         *
         * @param items the items to sort
         * @param getPartialDateTime lambda that receives a [T] and must return the [PartialDateTime] associated for the given element
         * @param additionalComparator in the case the [PartialDateTime]s of two items are identical, the given comparator is applied
         * @see PartialDateTime.sort
         */
        fun <T> sortAndGroup(
            items: Iterable<T>,
            getPartialDateTime: T.() -> PartialDateTime,
            additionalComparator: Comparator<T> = compareBy { 0 },
        ): Map<PartialDateTimeGroup, List<T>> {
            return sort(items, getPartialDateTime, additionalComparator).groupBy { it.getPartialDateTime().group() }
        }
    }
}

/**
 * Identical to [PartialDateTime.group], but can also return [PartialDateTimeGroup.Unknown] when [this] is `null`.
 */
fun PartialDateTime?.group(): PartialDateTimeGroup = when (this) {
    null -> PartialDateTimeGroup.Unknown
    else -> this.group()
}

/**
 * Sorts the given list of [PartialDateTime]. See [PartialDateTime.sort].
 */
fun List<PartialDateTime>.sort(): List<PartialDateTime> {
    return PartialDateTime.sort(items = this, getPartialDateTime = { this })
}
