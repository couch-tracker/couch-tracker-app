package io.github.couchtracker.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import java.util.Locale

val CompositionLocal<Configuration>.currentLocales
    @Composable
    @ReadOnlyComposable
    get(): LocaleListCompat {
        val configuration = this.current
        return ConfigurationCompat.getLocales(configuration)
    }

val CompositionLocal<Configuration>.currentFirstLocale
    @Composable
    @ReadOnlyComposable
    get(): Locale {
        return checkNotNull(currentLocales.get(0)) { "No locales available" }
    }
