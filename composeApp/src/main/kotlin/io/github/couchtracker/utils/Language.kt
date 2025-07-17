package io.github.couchtracker.utils

import androidx.annotation.StringRes
import com.ibm.icu.util.ULocale
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.Bcp47Language

sealed interface LanguageCategory {

    data object Special : LanguageCategory

    @JvmInline
    value class Language(val language: Bcp47Language) : LanguageCategory {
        init {
            require(language.isLanguageOnly()) { "Illegal category language, $language given" }
        }
    }
}

sealed interface LanguageItem {

    val language: Bcp47Language

    /**
     * Represents a "normal" language, that has no special meaning beyond identifying a specific language.
     */
    @JvmInline
    value class Normal(override val language: Bcp47Language) : LanguageItem

    /**
     * Represents a special language, as defined by ISO/BCP standards.
     *
     * These languages are hardcoded and don't represent any particular language, but represent a concept.
     */
    enum class Special(
        override val language: Bcp47Language,
        @StringRes val description: Int,
    ) : LanguageItem {
        NO_LINGUISTIC_CONTENT(Bcp47Language.of("zxx"), R.string.language_no_lingusitc_content_description),
        MULTIPLE_LANGUAGES(Bcp47Language.of("mul"), R.string.language_multilanguage_description),
        UNDETERMINED(Bcp47Language.of("und"), R.string.language_undetermined_description),
        ;

        companion object {
            val LANGUAGES = Special.entries.map { it.language }.toSet()
        }
    }
}

/**
 * Builds a tree of [Bcp47Language], which are grouped by base language.
 *
 * Each branch is sorted according to the given [comparator].
 */
fun Bcp47Language.Companion.languageTree(
    comparator: Comparator<MixedValueTree.NonRoot<LanguageCategory.Language, LanguageItem>>,
): MixedValueTree.Root<Unit, LanguageCategory, LanguageItem> {
    return MixedValueTree.Root(
        value = Unit,
        children = listOf(
            MixedValueTree.Intermediate(
                value = LanguageCategory.Special,
                children = LanguageItem.Special.entries.map { MixedValueTree.Leaf(it) },
            ),
        ).plus(
            ULocale.getAvailableLocales()
                .map { Bcp47Language(it) }
                .filter { it !in LanguageItem.Special.LANGUAGES }
                .groupBy { it.locale.language }
                .values
                .map { languages ->
                    if (languages.size == 1) {
                        MixedValueTree.Leaf(LanguageItem.Normal(languages.single()))
                    } else {
                        MixedValueTree.Intermediate(
                            value = LanguageCategory.Language(languages.single { it.isLanguageOnly() }),
                            children = languages
                                .map { MixedValueTree.Leaf(LanguageItem.Normal(it)) }
                                .sortedWith(comparator),
                        )
                    }
                }
                .sortedWith(comparator),
        ),
    )
}

fun Bcp47Language.isLanguageOnly(): Boolean {
    return locale.country.isNullOrBlank() && locale.variant.isNullOrBlank() && locale.script.isNullOrBlank()
}

fun Bcp47Language.Companion.getDefault(): Bcp47Language {
    return Bcp47Language(ULocale.getDefault().stripExtensions())
}
