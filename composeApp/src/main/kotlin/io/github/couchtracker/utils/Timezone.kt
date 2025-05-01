package io.github.couchtracker.utils

import kotlinx.datetime.TimeZone

sealed interface TimezoneItem {
    data class Zone(val leafName: String, val timezone: TimeZone) : TimezoneItem
    sealed interface Category : TimezoneItem {
        val items: List<TimezoneItem>
        fun totalItemCount(): Int {
            return items.sumOf {
                when (it) {
                    is Zone -> 1
                    is Category -> it.totalItemCount()
                }
            }
        }

        fun allTimeZones(): List<Zone> {
            return items.flatMap {
                when (it) {
                    is Zone -> listOf(it)
                    is Category -> it.allTimeZones()
                }
            }
        }

        data class Root(override val items: List<TimezoneItem>) : Category
        data class Uncategorized(override val items: List<TimezoneItem>) : Category
        data class GeographicalArea(val name: String, override val items: List<TimezoneItem>) : Category
    }
}

private data class NamedTimezone(val timezone: TimeZone, val nameParts: List<String>) {
    init {
        require(nameParts.isNotEmpty())
    }
}

/**
 * Timezone categories that should belong  in the uncategorized section
 */
private val OTHER_TIMEZONE_CATEGORIES = setOf("Antarctica", "Arctic", "Etc", "SystemV").map { it.uppercase() }

private fun denormalizeName(name: String): String {
    return name.replace('_', ' ')
}

/**
 * @return all available timezones, sorted and grouped in a tree-like format
 */
fun TimeZone.Companion.timezonesTree(): TimezoneItem.Category.Root {
    val timezones = TimeZone.availableZoneIds.map {
        NamedTimezone(TimeZone.of(it), it.split("/"))
    }
    val (uncategorized, categorized) = toTimezoneItems(timezones).partition { item ->
        when (item) {
            is TimezoneItem.Zone -> true
            is TimezoneItem.Category.GeographicalArea -> item.name.uppercase() in OTHER_TIMEZONE_CATEGORIES
            is TimezoneItem.Category.Root -> error("Unexpected item")
            is TimezoneItem.Category.Uncategorized -> error("Unexpected item")
        }
    }

    val uncategorizedNodes = TimezoneItem.Category.Uncategorized(uncategorized)
    val items = categorized.plus(uncategorizedNodes)
    return TimezoneItem.Category.Root(items.sort())
}

private fun toTimezoneItems(timezones: Collection<NamedTimezone>): List<TimezoneItem> {
    require(timezones.isNotEmpty())
    val (leaves, nodes) = timezones.partition { it.nameParts.size == 1 }

    val childZones = leaves
        .map { TimezoneItem.Zone(denormalizeName(it.nameParts.single()), it.timezone) }

    val childCategories = nodes
        .groupBy { it.nameParts.first() }
        .map { (groupKey, zones) ->
            TimezoneItem.Category.GeographicalArea(
                name = denormalizeName(groupKey),
                toTimezoneItems(
                    zones.map { namedTz ->
                        namedTz.copy(nameParts = namedTz.nameParts.subList(1, namedTz.nameParts.size))
                    },
                ),
            )
        }

    return (childZones + childCategories).sort()
}

private fun Collection<TimezoneItem>.sort(): List<TimezoneItem> {
    return sortedWith(
        compareBy<TimezoneItem> {
            @Suppress("MagicNumber")
            when (it) {
                is TimezoneItem.Category.Root -> 0
                is TimezoneItem.Category.GeographicalArea -> 1
                is TimezoneItem.Zone -> 2
                is TimezoneItem.Category.Uncategorized -> 3
            }
        }.thenBy {
            when (it) {
                is TimezoneItem.Category.GeographicalArea -> it.name
                is TimezoneItem.Zone -> it.leafName
                is TimezoneItem.Category.Root -> null
                is TimezoneItem.Category.Uncategorized -> null
            }
        },
    )
}
