package io.github.couchtracker

import android.content.Context
import androidx.core.os.LocaleListCompat
import io.github.couchtracker.utils.currentLocalesFlow
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.core.qualifier.named
import org.koin.dsl.module

object CurrentLocales

val AppModule = module {

    @OptIn(DelicateCoroutinesApi::class)
    single(named<CurrentLocales>()) {
        get<Context>().currentLocalesFlow().stateIn(GlobalScope, SharingStarted.Eagerly, LocaleListCompat.getDefault())
    }
}
