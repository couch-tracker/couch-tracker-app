package io.github.couchtracker.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

class TimezoneTest : FunSpec(
    {
        test("timezonesTree") {
            val root = TimeZone.timezonesTree()
            // Has all TZs
            root.totalItemCount() shouldBe TimeZone.availableZoneIds.size
            // has the uncategorized node
            val uncategorized = root.items.single { it is TimezoneItem.Category.Uncategorized } as TimezoneItem.Category.Uncategorized
            uncategorized.items.shouldNotBeEmpty()
            root.items.last() shouldBe uncategorized
            // Has categorized elements
            val categorized = root.items.filter { it !is TimezoneItem.Category.Uncategorized }
            // Has Europe
            val europe = categorized.single {
                it is TimezoneItem.Category.GeographicalArea && it.name == "Europe"
            } as TimezoneItem.Category.GeographicalArea
            // Has Isle of man
            val tz = TimeZone.of("Europe/Isle_of_Man")
            val isle = europe.items.single { it is TimezoneItem.Zone && it.timezone == tz } as TimezoneItem.Zone
            isle.leafName shouldBe "Isle of Man"
        }
    },
)
