package io.github.couchtracker.intl.datetime

import androidx.compose.ui.text.intl.Locale
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime.Local
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime.Zoned
import io.github.couchtracker.intl.Localized
import io.github.couchtracker.intl.formatDateTimeSkeleton
import kotlinx.datetime.TimeZone

/**
 * Specialization of [Localized] for [PartialDateTime] that are localized with a [Skeleton].
 */
abstract class LocalizedSkeletonPartialDateTime<out PDT : PartialDateTime>(item: PDT) : Localized<PDT>(item) {
    abstract val skeleton: Skeleton
}

private class LocalizedSkeletonPartialDateTimeImpl<out PDT : PartialDateTime>(
    item: PDT,
    override val skeleton: Skeleton,
) : LocalizedSkeletonPartialDateTime<PDT>(item) {

    override fun localize(locale: Locale): String {
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
            skeleton = skeleton,
            locale = locale,
        )
    }
}

/**
 * Localizes this [PartialDateTime.Local] with the given skeletons, which are concatenated together.
 */
fun <L : Local> L.localized(skeletons: Collection<LocalSkeleton<L>>): LocalizedSkeletonPartialDateTime<L> {
    return LocalizedSkeletonPartialDateTimeImpl(this, skeletons.sum())
}

fun <L : Local> L.localized(
    firstSkeleton: LocalSkeleton<L>,
    vararg otherSkeletons: LocalSkeleton<L>,
): LocalizedSkeletonPartialDateTime<L> {
    return LocalizedSkeletonPartialDateTimeImpl(this, (listOf(firstSkeleton) + otherSkeletons).toList().sum())
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
    val list = listOf(localSkeletons(this.local).skeleton, timezoneSkeleton).sum()
    return LocalizedSkeletonPartialDateTimeImpl(this, list)
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
