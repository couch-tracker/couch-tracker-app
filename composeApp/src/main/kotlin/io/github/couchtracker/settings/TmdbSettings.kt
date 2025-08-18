package io.github.couchtracker.settings

import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.couchtracker.SystemLocales
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbLanguages
import io.github.couchtracker.tmdb.toTmdbLanguage
import io.github.couchtracker.utils.toList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.collections.distinct
import kotlin.collections.ifEmpty
import kotlin.collections.take

object TmdbSettings : AbstractAppSettings(), KoinComponent {

    private val systemLocales by inject<StateFlow<LocaleListCompat>>(named<SystemLocales>())

    val Languages = setting(
        key = stringPreferencesKey("tmdbLanguages"),
        default = systemLocales.map { localeListCompat ->
            localeListCompat.toList()
                .mapNotNull { it.toTmdbLanguage() }
                .ifEmpty { listOf(TmdbLanguage.FALLBACK) }
                .distinct()
                .take(TmdbLanguages.MAX_LANGUAGES)
                .let(::TmdbLanguages)
        },
        parse = TmdbLanguages::parse,
        serialize = { it.serialize() },
    )
}
