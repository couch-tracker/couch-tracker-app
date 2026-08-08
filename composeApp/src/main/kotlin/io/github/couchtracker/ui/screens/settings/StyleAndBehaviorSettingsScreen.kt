package io.github.couchtracker.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import io.github.couchtracker.R
import io.github.couchtracker.settings.StyleAndBehaviorSettings.EpisodeNumberFormattingOption
import io.github.couchtracker.settings.StyleAndBehaviorSettings.OpenEpisodeBehaviorOption
import io.github.couchtracker.settings.StyleAndBehaviorSettings.OpenEpisodeBehaviorOption.HIGHLIGHT_IN_SEASON
import io.github.couchtracker.settings.StyleAndBehaviorSettings.OpenEpisodeBehaviorOption.OPEN_EPISODE_DETAILS
import io.github.couchtracker.settings.appSettings
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.PreferenceCategory

@Serializable
data object StyleAndBehaviorSettingsScreen : Screen() {

    override fun profileDataContext() = false

    @Composable
    override fun Content() {
        ScreenContainer { SettingsContent() }
    }
}

private const val EXAMPLE_SEASON = 1
private const val EXAMPLE_EPISODE = 2

@Composable
private fun SettingsContent() {
    val settings = appSettings()
    val cs = rememberCoroutineScope()
    val openEpisodeBehavior by settings.get { StyleAndBehavior.OpenEpisodeBehavior }
    val episodeFormatting by settings.get { StyleAndBehavior.EpisodeNumberFormatting }
    BaseSettings(
        title = R.string.style_and_behavior.str(),
        header = null,
        footer = null,
    ) {
        item("style-category") { PreferenceCategory(title = { Text(R.string.style.str()) }) }
        item("abbreviated-episode-format") {
            ListPreference(
                value = episodeFormatting,
                onValueChange = {
                    cs.launch {
                        settings.getSetting { StyleAndBehavior.EpisodeNumberFormatting }.set(it)
                    }
                },
                values = EpisodeNumberFormattingOption.entries.toList(),
                title = {
                    Text(R.string.episode_number_formatting.str())
                },
                summary = {
                    val example = episodeFormatting.format(EXAMPLE_SEASON, EXAMPLE_EPISODE)
                    Text(R.string.episode_number_formatting_summary.str(example))
                },
                icon = {
                    Text(
                        "SxE",
                        style = MaterialTheme.typography.labelLarge,
                        fontFamily = FontFamily.Monospace,
                        color = LocalContentColor.current,
                    )
                },
                type = ListPreferenceType.ALERT_DIALOG,
                valueToText = {
                    val example = it.format(EXAMPLE_SEASON, EXAMPLE_EPISODE)
                    AnnotatedString(example)
                },
            )
        }
        item("behavior-category") { PreferenceCategory(title = { Text(R.string.behavior.str()) }) }
        item("open-episode-behavior") {
            val nameStrings = OpenEpisodeBehaviorOption.entries.associateWith {
                when (it) {
                    HIGHLIGHT_IN_SEASON -> R.string.open_episode_behavior_highlight_in_season.str()
                    OPEN_EPISODE_DETAILS -> R.string.open_episode_behavior_episode_details.str()
                }
            }
            ListPreference(
                value = openEpisodeBehavior,
                onValueChange = {
                    cs.launch {
                        settings.getSetting { StyleAndBehavior.OpenEpisodeBehavior }.set(it)
                    }
                },
                values = OpenEpisodeBehaviorOption.entries.toList(),
                title = {
                    Text(R.string.open_episode_behavior.str())
                },
                summary = {
                    Text(nameStrings.getValue(openEpisodeBehavior))
                },
                icon = {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                },
                type = ListPreferenceType.ALERT_DIALOG,
                valueToText = {
                    AnnotatedString(nameStrings.getValue(it))
                },
            )
        }
    }
}
