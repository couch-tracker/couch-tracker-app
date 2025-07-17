package io.github.couchtracker.intl.datetime

import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime

/**
 * Calls [localized] as with the fullest (and longest) amount of information.
 */
fun PartialDateTime.localizedFull(): LocalizedSkeletonPartialDateTime<PartialDateTime> {
    return localized(
        timezoneSkeleton = TimezoneSkeleton.TIMEZONE_ID,
        localSkeletons = {
            when (it) {
                is PartialDateTime.Local.Year -> it.localized(YearSkeleton.NUMERIC)
                is PartialDateTime.Local.YearMonth -> it.localized(YearSkeleton.NUMERIC, MonthSkeleton.WIDE)
                is PartialDateTime.Local.Date -> it.localized(Skeletons.FULL_DATE)
                is PartialDateTime.Local.DateTime -> it.localized(Skeletons.FULL_DATE + TimeSkeleton.MINUTES)
            }
        },
    )
}
