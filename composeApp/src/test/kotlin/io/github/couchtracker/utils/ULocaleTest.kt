package io.github.couchtracker.utils

import com.ibm.icu.util.ULocale
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class ULocaleTest : FunSpec(
    {
        context("stripExtensions") {
            context("returns expected locale") {
                withData(
                    "en" to "en",
                    "en_IE" to "en_IE",
                    "es_ES@currency=EUR;collation=traditional" to "es_ES",
                    "it_IT#U_FW_MON_MU_CELSIUS@attribute=email;collation=phonebook;x=linux" to "it_IT#U_FW_MON_MU_CELSIUS",
                ) { (locale, expected) ->
                    ULocale(locale).stripExtensions() shouldBe ULocale(expected)
                }
            }

            context("doesn't break any existing locale") {
                withData(
                    nameFn = { it.toString() },
                    LocaleData().allLocales.filter { it.extensionKeys.isEmpty() }.toList(),
                ) { locale ->
                    locale.stripExtensions() shouldBe locale
                }
            }
        }
    },
)
