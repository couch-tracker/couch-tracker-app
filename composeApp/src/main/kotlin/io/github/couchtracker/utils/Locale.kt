package io.github.couchtracker.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.ibm.icu.util.ULocale
import org.koin.core.module.dsl.singleOf
import java.util.Locale

class LocaleData {
    @Suppress("ForbiddenMethodCall")
    val allLocales = ULocale.getAvailableLocales().asList()
}

val LocaleModule = lazyEagerModule {
    singleOf(::LocaleData)
}

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

fun Locale.toULocale() = ULocale(toString())

fun LocaleListCompat.toList(): List<Locale> {
    return List(size()) { i -> checkNotNull(get(i)) }
}
