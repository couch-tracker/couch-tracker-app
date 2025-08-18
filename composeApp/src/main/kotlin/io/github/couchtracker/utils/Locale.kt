package io.github.couchtracker.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.ibm.icu.util.ULocale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
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

/**
 * Creates a new [Flow] that listens to [Intent.ACTION_LOCALE_CHANGED] and produces values when the system locale list changes.
 */
fun Context.currentLocalesFlow(): Flow<LocaleListCompat> = callbackFlow {
    val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = runBlocking {
            send(LocaleListCompat.getDefault())
        }
    }
    registerReceiver(broadcastReceiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED))

    awaitClose {
        unregisterReceiver(broadcastReceiver)
    }
}
