package io.github.couchtracker.intl.datetime

import com.ibm.icu.util.ULocale
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime.Local
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime.Zoned
import io.github.couchtracker.intl.Localized
import io.github.couchtracker.intl.formatDateTimeSkeleton
import kotlinx.datetime.TimeZone

/**
 * Specialization of [Localized] for [PartialDateTime] that are localized with a [Skeleton].
 */
abstract class LocalizedSkeletonPartialDateTime<out PDT : PartialDateTime>(item: PDT) : Localized<PDT>(item) {
    abstract val dateTimeSkeleton: DateTimeSkeleton
    abstract val timezoneSkeleton: TimezoneSkeleton?
}

private class LocalizedSkeletonPartialDateTimeImpl<out PDT : PartialDateTime>(
    item: PDT,
    override val dateTimeSkeleton: DateTimeSkeleton,
    override val timezoneSkeleton: TimezoneSkeleton?,
) : LocalizedSkeletonPartialDateTime<PDT>(item) {

    override fun localize(locale: ULocale): String {
        val (instant, timeZone) = when (val pdt = item as PartialDateTime) {
            is Local -> {
                val defaultTimeZone = TimeZone.currentSystemDefault()
                pdt.toInstant(defaultTimeZone) to defaultTimeZone
            }

            is Zoned -> pdt.toInstant() to pdt.zone
        }

        return formatDateTimeSkeleton(
            instant = instant,
            timeZone = timeZone,
            dateTimeSkeleton = dateTimeSkeleton,
            timezoneSkeleton = timezoneSkeleton,
            locale = locale,
        )
    }
}

fun <L : Local.WithYear> L.localized(yearSkeleton: YearSkeleton): LocalizedSkeletonPartialDateTime<L> {
    return LocalizedSkeletonPartialDateTimeImpl(this, DateTimeSkeleton(yearSkeleton), timezoneSkeleton = null)
}

fun <L : Local.WithYearMonth> L.localized(yearSkeleton: YearSkeleton?, monthSkeleton: MonthSkeleton?): LocalizedSkeletonPartialDateTime<L> {
    return LocalizedSkeletonPartialDateTimeImpl(this, DateTimeSkeleton(yearSkeleton, monthSkeleton), timezoneSkeleton = null)
}

fun <L : Local.WithDate> L.localized(skeleton: DateSkeleton): LocalizedSkeletonPartialDateTime<L> {
    return LocalizedSkeletonPartialDateTimeImpl(this, DateTimeSkeleton(skeleton, time = null), timezoneSkeleton = null)
}

fun <L : Local.WithDateTime> L.localized(skeleton: DateTimeSkeleton): LocalizedSkeletonPartialDateTime<L> {
    return LocalizedSkeletonPartialDateTimeImpl(this, skeleton, timezoneSkeleton = null)
}

fun <L : Local.WithDateTime> L.localized(dateSkeleton: DateSkeleton?, timeSkeleton: TimeSkeleton?): LocalizedSkeletonPartialDateTime<L> {
    return localized(DateTimeSkeleton(dateSkeleton, timeSkeleton))
}

/**
 * Localizes a [PartialDateTime.Zoned].
 *
 * @param timezoneSkeleton the skeleton to use to format the timezone
 * @param localSkeletons a function to select the way to localize the inner [PartialDateTime.Local] instance.
 */
fun Zoned.localized(
    timezoneSkeleton: TimezoneSkeleton,
    localSkeletons: (Local) -> LocalizedSkeletonPartialDateTime<Local>,
): LocalizedSkeletonPartialDateTime<Zoned> {
    return LocalizedSkeletonPartialDateTimeImpl(this, localSkeletons(this.local).dateTimeSkeleton, timezoneSkeleton)
}

/**
 * Localizes a generic [PartialDateTime].
 *
 * If you know that your instance is a [PartialDateTime.Local] or a [PartialDateTime.Zoned], use the appropriate overloads.
 *
 * @param timezoneSkeleton the skeleton to use to format the timezone, if present. In case of a [PartialDateTime.Local] this is ignored.
 * @param localSkeletons a function to select how to localize a [PartialDateTime.Local], whether it's this instance or the one within a
 * [Zoned].
 */
fun PartialDateTime.localized(
    timezoneSkeleton: TimezoneSkeleton,
    localSkeletons: (Local) -> LocalizedSkeletonPartialDateTime<Local>,
): LocalizedSkeletonPartialDateTime<PartialDateTime> {
    return when (this) {
        is Local -> localSkeletons(this)
        is Zoned -> this.localized(timezoneSkeleton, localSkeletons)
    }
}
