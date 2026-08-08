package io.github.couchtracker.settings

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
}
