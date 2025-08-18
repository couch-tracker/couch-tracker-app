package io.github.couchtracker

import android.content.Context
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.preferencesDataStore
import io.github.couchtracker.utils.currentLocalesFlow
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.core.qualifier.named
import org.koin.dsl.lazyModule

object SystemLocales
object AppSettingsDataStore

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

val AppModule = lazyModule {
    @OptIn(DelicateCoroutinesApi::class)
    single(named<SystemLocales>()) {
        get<Context>().currentLocalesFlow().stateIn(GlobalScope, SharingStarted.Eagerly, LocaleListCompat.getDefault())
    }

    single(named<AppSettingsDataStore>()) {
        with(get<Context>()) {
            settingsDataStore
        }
    }
}
