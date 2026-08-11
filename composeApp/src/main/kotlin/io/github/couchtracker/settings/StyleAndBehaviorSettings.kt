package io.github.couchtracker.settings

import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import io.github.couchtracker.R
import io.github.couchtracker.settings.StyleAndBehaviorSettings.UpNextSortOrderOption
import io.github.couchtracker.utils.settings.LoadedSettings
import kotlinx.coroutines.flow.flowOf
import org.koin.core.component.KoinComponent
import java.util.Locale

object StyleAndBehaviorSettings : AbstractAppSettings(), KoinComponent {

    @Suppress("EnumNaming", "EnumEntryNameCase")
    enum class EpisodeNumberFormattingOption(
        private val template: String,
    ) {
        S01E02($$"S%1$02dE%2$02d"),
        sXXeYY($$"s%1$02de%2$02d"),
        XXxYY($$"%1$02dx%2$02d"),
        XxY($$"%1$dx%2$d"),
        ;

        fun format(seasonNumber: Int, episodeNumber: Int): String {
            return template.format(locale = Locale.ROOT, seasonNumber, episodeNumber)
        }
    }

    val EpisodeNumberFormatting = setting<EpisodeNumberFormattingOption>(
        key = "episodeNumberFormatting",
        default = EpisodeNumberFormattingOption.S01E02,
    )

    enum class OpenEpisodeBehaviorOption {
        HIGHLIGHT_IN_SEASON,
        OPEN_EPISODE_DETAILS,
    }

    val OpenEpisodeBehavior = setting<OpenEpisodeBehaviorOption>(
        key = "openEpisodeBehavior",
        default = OpenEpisodeBehaviorOption.HIGHLIGHT_IN_SEASON,
    )

    enum class UpNextSortOrderOption(@StringRes val stringRes: Int) {
        LAST_WATCHED_FIRST(R.string.up_next_sort_option_last_watched_first),
        SAME_AS_SHOWS(R.string.up_next_sort_option_same_as_shows),
        NEWEST_FIRST(R.string.up_next_sort_option_newest_first),
        OLDEST_FIRST(R.string.up_next_sort_option_oldest_first),
    }

    val UpNextEnableSuggestions = setting(
        key = booleanPreferencesKey("up-next-enable-suggestions"),
        default = flowOf(true),
    )
    val UpNextDivideAired = setting(
        key = booleanPreferencesKey("up-next-divide-aired"),
        default = flowOf(true),
    )
    val UpNextSortOrder = setting(
        key = "up-next-sort-order",
        default = UpNextSortOrderOption.LAST_WATCHED_FIRST,
    )
}

data class UpNextOptions(
    val enableSmartSuggestions: Boolean,
    val divideAired: Boolean,
    val sortOrder: UpNextSortOrderOption,
)

fun LoadedSettings<AppSettings>.upNextOptions(): UpNextOptions {
    val loaded = this
    return UpNextOptions(
        enableSmartSuggestions = loaded.get { StyleAndBehavior.UpNextEnableSuggestions }.current,
        divideAired = loaded.get { StyleAndBehavior.UpNextDivideAired }.current,
        sortOrder = loaded.get { StyleAndBehavior.UpNextSortOrder }.current,
    )
}
