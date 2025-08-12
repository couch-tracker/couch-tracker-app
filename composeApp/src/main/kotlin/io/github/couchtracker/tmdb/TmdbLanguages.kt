package io.github.couchtracker.tmdb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.couchtracker.CurrentLocales
import io.github.couchtracker.Settings
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.currentLocales
import io.github.couchtracker.utils.toList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.koin.core.context.GlobalContext
import org.koin.core.qualifier.named

/**
 * Class representing a list of [TmdbLanguages] to use to get the localized something of a TMDB item.
 *
 * Languages should be tried in order, and use the first available localized content.
 *
 * This class guarantees that the list is not empty, does not contain duplicates, and that does not exceed [TmdbLanguages.MAX_LANGUAGES].
 */
@JvmInline
value class TmdbLanguages(val languages: List<TmdbLanguage>) {

    init {
        require(languages.isNotEmpty()) { "TMDB language list must not be empty" }
        require(languages.size <= MAX_LANGUAGES) { "TMDB language list too big: ${languages.size}, max is $MAX_LANGUAGES" }
        require(languages.distinct().size == languages.size) { "TMDB language list contains duplicates: $languages" }
    }

    val size get() = languages.size

    val first get() = languages.first()

    fun tryPlus(another: TmdbLanguage) = TmdbLanguages((languages + another).distinct())

    fun tryMinus(another: TmdbLanguage) = if (size > 1) TmdbLanguages(languages - another) else this

    fun withMovedItem(fromIndex: Int, toIndex: Int): TmdbLanguages {
        require(fromIndex in languages.indices)
        require(toIndex in languages.indices)

        return TmdbLanguages(
            languages = languages.toMutableList().apply {
                add(index = toIndex, element = removeAt(fromIndex))
            },
        )
    }

    fun isMaxLanguages() = size == MAX_LANGUAGES

    fun serialize() = languages.joinToString(separator = ",") { it.serialize() }

    companion object {
        const val MAX_LANGUAGES = 5

        fun parse(value: String): TmdbLanguages {
            return TmdbLanguages(value.split(",").map { TmdbLanguage.parse(it) })
        }
    }
}

private fun LocaleListCompat.toTmdbLanguages(): TmdbLanguages {
    return toList()
        .mapNotNull { it.toTmdbLanguage() }
        .ifEmpty { listOf(TmdbLanguage.FALLBACK) }
        .distinct()
        .take(TmdbLanguages.MAX_LANGUAGES)
        .let(::TmdbLanguages)
}

@Composable
fun TmdbLanguages?.orDefault(): TmdbLanguages {
    val systemTmdbLanguages = LocalConfiguration.currentLocales.toTmdbLanguages()
    return this ?: systemTmdbLanguages
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Flow<TmdbLanguages?>.orDefault(): Flow<TmdbLanguages> {
    return flatMapLatest { languages ->
        if (languages != null) {
            flowOf(languages)
        } else {
            GlobalContext.get().get<StateFlow<LocaleListCompat>>(named<CurrentLocales>()).map { it.toTmdbLanguages() }
        }
    }
}

val LocalTmdbLanguagesContext = compositionLocalOf<TmdbLanguages> { error("no default TmdbLanguages") }

@Composable
fun TmdbLanguagesContext(content: @Composable () -> Unit) {
    val flow = remember { Settings.TmdbLanguages.orDefault().map { Result.Value(it) } }
    val tmdbLanguages by flow.collectAsStateWithLifecycle(Loadable.Loading)

    LoadableScreen(tmdbLanguages) { tmdbLanguages ->
        CompositionLocalProvider(LocalTmdbLanguagesContext provides tmdbLanguages) {
            content()
        }
    }
}
