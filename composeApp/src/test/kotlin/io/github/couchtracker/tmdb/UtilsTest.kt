package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbMovieDetail
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.utils.LocaleData
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class UtilsTest : FunSpec(
    {
        context("TmdbDetail.language") {
            withData(
                nameFn = { "originalLanguage=${it.a}, originCountry=${it.b} should return ${it.c} " },
                tuple("it", emptyList(), Bcp47Language.of("it")),
                tuple("it", listOf("IT"), Bcp47Language.of("it-IT")),
                tuple("it", listOf("IE", "CH", "IT"), Bcp47Language.of("it-CH")),
                tuple("it", listOf("JP", "ZH", "UK"), Bcp47Language.of("it")),
            ) { (lang, countries, expected) ->
                val detail = mockk<TmdbMovieDetail> {
                    every { originalLanguage } returns lang
                    every { originCountry } returns countries
                }
                detail.language(LocaleData().allLocales) shouldBe expected
            }
        }
    },
)
