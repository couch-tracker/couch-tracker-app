package io.github.couchtracker.db.profile

import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.LocaleData
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.lang.IllegalArgumentException

class Bcp47LanguageTest : FunSpec(
    {
        context("constructor") {
            context("fails with invalid locales") {
                withData(
                    nameFn = { it.toString().ifBlank { "<blank>" } },
                    // Invalid language
                    ULocale("x"),
                    ULocale("   "),

                    // Invalid variant
                    ULocale("it_IT_#U_FW_MON_MU_CELSIUS"),

                    // Contains extensions
                    ULocale("es_ES@currency=EUR;collation=traditional"),
                    ULocale("de_DE@attribute=email;collation=phonebook;x=linux"),
                ) { locale ->
                    shouldThrow<IllegalArgumentException> {
                        Bcp47Language(locale)
                    }
                }
            }

            context("succeeds with all available locales and special ones") {
                withData(
                    nameFn = { it.toString().ifEmpty { "<empty>" } },
                    LocaleData().allLocales + listOf(
                        ULocale("und"),
                        ULocale("mul"),
                        ULocale("zxx"),
                    ),
                ) { locale ->
                    shouldNotThrowAny {
                        Bcp47Language(locale)
                    }
                }
            }
        }

        context("of") {
            context("returns expected value") {
                withData(
                    nameFn = { it.first },
                    "it" to Bcp47Language(ULocale("it")),
                    "pt-BR" to Bcp47Language(ULocale("pt", "BR")),
                    "it-Latn-IT" to Bcp47Language(ULocale("it", "Latn", "IT")),
                    "en-US-u-va-posix" to Bcp47Language(ULocale("en", "US", "POSIX")),
                ) { (value, expected) ->
                    Bcp47Language.of(value) shouldBe expected
                }
            }
            context("fails on invalid inputs") {
                ->
                withData(
                    nameFn = { it.ifEmpty { "<empty>" }.ifBlank { "<blank>" } },
                    "",
                    "  ",
                    "not a valid language tag",
                    "it_IT", // Invalid separator
                    "xxxxxxxxxxxxxxxxxxxx",
                ) { value ->
                    val thrown = shouldThrow<IllegalArgumentException> {
                        Bcp47Language.of(value)
                    }
                    thrown.message shouldContain value
                }
            }
        }

        context("toLossyBcp47Language") {
            withData(
                nameFn = { it.first.toString() },
                ULocale("it_IT_#U_FW_MON_MU_CELSIUS") to Bcp47Language.of("it-IT"),
                ULocale("es_ES@currency=EUR;collation=traditional") to Bcp47Language.of("es-ES"),
                ULocale("de_DE@attribute=email;collation=phonebook;x=linux") to Bcp47Language.of("de-DE"),
            ) { (locale, expectedLanguage) ->
                locale.toLossyBcp47Language() shouldBe expectedLanguage
            }
        }
    },
)
