package io.github.couchtracker.db.profile

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.LocaleDisplayNames
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.stripExtensions

/**
 * This class represents a language, as per the [IETF BCP 47 standard](https://en.wikipedia.org/wiki/IETF_language_tag).
 *
 * It wraps a [ULocale], disallowing anything that doesn't represent an IETF BCP 47 language tag.
 */
@JvmInline
value class Bcp47Language(val locale: ULocale) {
    init {
        // Extensions don't add identifying information about a language, but are just supplementary information
        // For this reason, we want to disallow them from this class
        require(locale.extensionKeys.isEmpty()) {
            "The given ULocale ($locale) contains extensions, which are not allowed in Bcp47Language"
        }
        // If the round trip doesn't yield an identical locale, it means that it contains invalid information which was either transformed
        // or omitted by the toLanguageTag() call.
        // This means that the given ULocale does not represent a valid BCP47 language tag
        require(ULocale.forLanguageTag(locale.toLanguageTag()) == locale) {
            "The given ULocale ($locale) does not represent a valid BCP47 language tag"
        }
    }

    infix fun isEqualTo(another: Bcp47Language): Boolean {
        return locale == another.locale
    }

    override fun toString(): String = locale.toLanguageTag()

    fun getDisplayName(locale: ULocale, capitalization: DisplayContext): String {
        require(capitalization.type() == DisplayContext.Type.CAPITALIZATION)
        return LocaleDisplayNames.getInstance(locale, capitalization).localeDisplayName(this.locale)
    }

    companion object {
        fun of(languageTag: String): Bcp47Language {
            require(languageTag.isNotEmpty()) { "invalid empty language tag" }

            val locale = ULocale.forLanguageTag(languageTag)
            if (languageTag != "und") {
                // ULocale with empty language means `und`
                // Any string passed to forLanguageTag that is no recognized/value, will return ULocale("")
                // Unless we wanted to create the language `und` explicitly, we need to fail
                require(locale.language.isNotBlank()) {
                    "The given value ($languageTag) is not a valid IANA BCP47 language tag"
                }
            }
            return Bcp47Language(locale)
        }
    }
}

/**
 * Converts this [ULocale] to a [Bcp47Language], possibly losing information that is not representable in [Bcp47Language].
 */
fun ULocale.toLossyBcp47Language(): Bcp47Language {
    return Bcp47Language(ULocale.forLanguageTag(this.stripExtensions().toLanguageTag()))
}
