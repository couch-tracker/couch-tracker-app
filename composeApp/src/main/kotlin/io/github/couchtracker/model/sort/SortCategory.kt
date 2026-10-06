package io.github.couchtracker.model.sort

import android.content.Context
import android.icu.text.MessageFormat
import android.icu.util.ULocale
import dev.mmauro.datetimepolyglot.localizers.absolute.localize
import io.github.couchtracker.R
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

interface SortCategory {
    fun localize(context: Context): String?

    object RootCategory : SortCategory {
        override fun localize(context: Context) = null
    }

    data class NameSortCategory(val initial: Char) : SortCategory {
        override fun localize(context: Context) = initial.toString()
    }

    data class TmdbRatingCategory(val stars: Int) : SortCategory {
        override fun localize(context: Context) = context.getString(R.string.sort_category_rating, stars)
    }

    data class FirstPublicReleaseCenturyCategory(val century: Int) : SortCategory {
        override fun localize(context: Context): String {
            val locale = context.resources.configuration.locales[0]
            val formatter = MessageFormat("{0,ordinal}", locale)

            @Suppress("ArrayPrimitive")
            val centuryStr = formatter.format(arrayOf(century))
            return context.getString(R.string.sort_category_release_century, centuryStr)
        }
    }

    data class FirstPublicReleaseDecadeCategory(
        /** The year when the decade started */
        val decadeStartYear: Int,
    ) : SortCategory {
        override fun localize(context: Context): String {
            return context.getString(R.string.sort_category_release_decade, decadeStartYear)
        }
    }

    data class FirstPublicReleaseYearCategory(val year: Int) : SortCategory {
        override fun localize(context: Context): String {
            return context.getString(R.string.sort_category_release_year, year)
        }
    }

    data class FirstPublicReleaseYearMonthCategory(val yearMonth: YearMonth) : SortCategory {
        override fun localize(context: Context): String {
            val locale = context.resources.configuration.locales[0]
            val uLocale = ULocale.forLocale(locale)
            return context.getString(R.string.sort_category_release_year_month, yearMonth.localize(locale = uLocale))
        }
    }

    data class FirstPublicReleaseDateCategory(val date: LocalDate) : SortCategory {
        override fun localize(context: Context): String {
            val locale = context.resources.configuration.locales[0]
            val uLocale = ULocale.forLocale(locale)
            return context.getString(R.string.sort_category_release_date, date.localize(locale = uLocale))
        }
    }

    data object Error : SortCategory {
        override fun localize(context: Context) = context.getString(R.string.sort_category_error)
    }

    data object Unsupported : SortCategory {
        override fun localize(context: Context) = context.getString(R.string.sort_category_unsupported)
    }

    data class Unknown(val sortCategory: Sorter) : SortCategory {
        override fun localize(context: Context): String {
            return when (sortCategory) {
                is Sorter.Alphabetical -> context.getString(R.string.sort_category_without_name)
                is Sorter.ByReleaseDate -> context.getString(R.string.sort_category_without_release_date)
                is Sorter.ByTmdbRating -> context.getString(R.string.sort_category_without_rating)
            }
        }
    }
}
