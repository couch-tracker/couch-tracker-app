package io.github.couchtracker.utils

import kotlinx.datetime.TimeZone

/**
 * Timezone categories that should belong  in the uncategorized section
 */
private val OTHER_TIMEZONE_CATEGORIES = setOf("Antarctica", "Arctic", "Etc", "SystemV").map { it.uppercase() }

sealed interface TimeZoneCategory {
    data class GeographicalArea(val name: String) : TimeZoneCategory
    data object Uncategorized : TimeZoneCategory
}

data class NamedTimezone(val timezone: TimeZone, val nameParts: List<String>) {

    init {
        require(nameParts.isNotEmpty())
    }

    val leafName = denormalizeName(nameParts.last())
}

private fun denormalizeName(name: String): String {
    return name.replace('_', ' ')
}

/**
 * @return all available timezones, sorted and grouped in a tree-like format
 */
fun TimeZone.Companion.timezonesTree(): MixedValueTree.Root<Unit, TimeZoneCategory, NamedTimezone> {
    val timezones = TimeZone.availableZoneIds.map {
        NamedTimezone(TimeZone.of(it), it.split("/"))
    }
    val (uncategorized, categorized) = toTimezoneItems(timezones).partition { item ->
        when (item) {
            is MixedValueTree.Leaf -> true
            is MixedValueTree.Intermediate -> item.value.name.uppercase() in OTHER_TIMEZONE_CATEGORIES
        }
    }

    val uncategorizedNodes = MixedValueTree.Intermediate(value = TimeZoneCategory.Uncategorized, children = uncategorized)
    val items = categorized.plus(uncategorizedNodes)
    return MixedValueTree.Root(value = Unit, items.sortedWith(COMPARATOR))
}

private fun toTimezoneItems(
    timezones: Collection<NamedTimezone>,
): List<MixedValueTree.NonRoot<TimeZoneCategory.GeographicalArea, NamedTimezone>> {
    require(timezones.isNotEmpty())
    val (leaves, nodes) = timezones.partition { it.nameParts.size == 1 }

    val childZones = leaves
        .map { MixedValueTree.Leaf(it) }

    val childCategories = nodes
        .groupBy { it.nameParts.first() }
        .map { (groupKey, zones) ->
            MixedValueTree.Intermediate(
                value = TimeZoneCategory.GeographicalArea(denormalizeName(groupKey)),
                children = toTimezoneItems(
                    zones.map { namedTz ->
                        namedTz.copy(nameParts = namedTz.nameParts.subList(1, namedTz.nameParts.size))
                    },
                ),
            )
        }

    return (childZones + childCategories).sortedWith(COMPARATOR)
}

private val COMPARATOR = compareBy<MixedValueTree<Unit, TimeZoneCategory, NamedTimezone>> {
    @Suppress("MagicNumber")
    when (it) {
        is MixedValueTree.Root<*, *, *> -> 0
        is MixedValueTree.Leaf -> 2
        is MixedValueTree.Intermediate -> when (it.value) {
            is TimeZoneCategory.GeographicalArea -> 1
            TimeZoneCategory.Uncategorized -> 3
        }
    }
}.thenBy {
    when (it) {
        is MixedValueTree.Leaf -> it.value.leafName
        is MixedValueTree.Root<*, *, *> -> null
        is MixedValueTree.Intermediate -> when (it.value) {
            is TimeZoneCategory.GeographicalArea -> it.value.name
            TimeZoneCategory.Uncategorized -> null
        }
    }
}
