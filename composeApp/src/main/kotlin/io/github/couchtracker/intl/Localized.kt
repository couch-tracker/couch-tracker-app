package io.github.couchtracker.intl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.intl.Locale
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.toULocale

/**
 * Abstract class which holds an [item] of type [T] that can be localized by calling the [localize] method.
 */
abstract class Localized<out T>(val item: T) : Text {

    /**
     * Returns the localized string representing [item] with the given [locale], or the default one if not provided.
     */
    abstract fun localize(locale: ULocale = Locale.current.platformLocale.toULocale()): String

    @Composable
    @ReadOnlyComposable
    override fun string() = localize()
}
