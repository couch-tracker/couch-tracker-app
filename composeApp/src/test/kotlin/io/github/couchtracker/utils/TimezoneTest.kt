package io.github.couchtracker.utils

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone

class TimezoneTest : FunSpec(
    {
        context("timezonesTree") {
            val root = TimeZone.timezonesTree()

            test("has all TZs") {
                root.countLeafs() shouldBe TimeZone.availableZoneIds.size
            }

            test("uncategorized") {
                val uncategorized = withClue("exists") {
                    root.children
                        .filterIsInstance<MixedValueTree.Intermediate<TimeZoneCategory, NamedTimezone>>()
                        .filter { it.value == TimeZoneCategory.Uncategorized }
                        .shouldBeSingleton()
                        .single()
                }

                withClue("has timezones") {
                    uncategorized.children.shouldNotBeEmpty()
                }

                withClue("is last element") {
                    root.children.last() shouldBe uncategorized
                }
            }
            test("categorized") {
                val categorized = withClue("exist") {
                    root.children
                        .filterIsInstance<MixedValueTree.Intermediate<TimeZoneCategory, NamedTimezone>>()
                        .filter { it.value != TimeZoneCategory.Uncategorized }
                        .shouldHaveAtLeastSize(2)
                }

                val europe = withClue("has Europe") {
                    categorized
                        .filter { it.value is TimeZoneCategory.GeographicalArea && it.value.name == "Europe" }
                        .shouldBeSingleton()
                }.single()

                withClue("has Isle of man") {
                    val tz = TimeZone.of("Europe/Isle_of_Man")
                    val isle = europe.children
                        .filterIsInstance<MixedValueTree.Leaf<NamedTimezone>>()
                        .filter { it.value.timezone == tz }
                        .shouldBeSingleton()
                        .single()

                    isle.value.leafName shouldBe "Isle of Man"
                }
            }
        }
    },
)
