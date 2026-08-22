package io.github.couchtracker.intl.datetime

import android.content.Context
import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import dev.mmauro.datetimepolyglot.localizers.PolyglotLocalizer
import dev.mmauro.datetimepolyglot.localizers.PolyglotLocalizerOptions
import io.github.couchtracker.utils.toAndroidULocale

@Composable
fun <OPTIONS : PolyglotLocalizerOptions<LOCALIZER>, LOCALIZER : PolyglotLocalizer> rememberLocalizer(options: OPTIONS): LOCALIZER {
    val locale = LocalLocale.current.platformLocale
    return remember(options, locale) { options.localizer(locale.toAndroidULocale()) }
}

@Composable
fun <OPTIONS, LOCALIZER> rememberLocalizer(options: OPTIONS, localizerFactory: (Context, OPTIONS, ULocale) -> LOCALIZER): LOCALIZER {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    return remember(context, options, locale, localizerFactory) { localizerFactory(context, options, locale.toAndroidULocale()) }
}
