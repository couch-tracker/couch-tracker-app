package io.github.couchtracker.db.user.model.partialtime

/**
 * When showing a list of objects with an associated [PartialDateTime], they should be split into groups in order to provide a clear UI to
 * the user.
 *
 * Furthermore, splitting the object into distincts groups is a key factor into how they are sorted.
 *
 * @see PartialDateTime.Local.Year
 * @see PartialDateTime.Local.YearMonth
 */
sealed class PartialDateTimeGroup : Comparable<PartialDateTimeGroup> {

    /**
     * Used only for those [PartialDateTime]s where only the only local part known is the year
     */
    data class Year(val value: PartialDateTime.Local.Year) : PartialDateTimeGroup()

    /**
     * Used only for all [PartialDateTime]s that don't fit into [Year] or [Unknown] groups.
     */
    data class YearMonth(val value: PartialDateTime.Local.YearMonth) : PartialDateTimeGroup()

    /**
     * Used when the [PartialDateTime] of an object is unknown.
     */
    data object Unknown : PartialDateTimeGroup()

    override fun compareTo(other: PartialDateTimeGroup) = COMPARATOR.compare(this, other)

    companion object {
        private val COMPARATOR: Comparator<PartialDateTimeGroup> = compareBy<PartialDateTimeGroup> {
            // All unknowns go first
            when (it) {
                Unknown -> 0
                else -> 1
            }
        }.thenBy {
            when (it) {
                Unknown -> 0
                is Year -> it.value.year
                is YearMonth -> it.value.year
            }
        }.thenBy {
            when (it) {
                Unknown, is Year -> 0
                is YearMonth -> it.value.month.value // January is 1, December is 12
            }
        }
    }
}
