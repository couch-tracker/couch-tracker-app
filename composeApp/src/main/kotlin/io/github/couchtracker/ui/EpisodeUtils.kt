package io.github.couchtracker.ui

import android.content.Context
import io.github.couchtracker.R
import io.github.couchtracker.settings.StyleAndBehaviorSettings

fun seasonEpisodeNumberToString(
    context: Context,
    formatting: StyleAndBehaviorSettings.EpisodeNumberFormattingOption,
    seasonNumber: Int,
    episodeNumber: Int,
    episodeName: String? = null,
): String {
    return if (seasonNumber == 0) {
        if (episodeName == null) {
            context.getString(R.string.season_special_episode, episodeNumber)
        } else {
            context.getString(R.string.season_special_episode_with_name, episodeNumber, episodeName)
        }
    } else {
        val episodeFormatted = formatting.format(seasonNumber, episodeNumber)
        if (episodeName == null) {
            episodeFormatted
        } else {
            context.getString(R.string.season_episode_with_name, episodeFormatted, episodeName)
        }
    }
}

fun showSeasonEpisodeNumberToString(
    context: Context,
    formatting: StyleAndBehaviorSettings.EpisodeNumberFormattingOption,
    showName: String?,
    seasonNumber: Int,
    episodeNumber: Int,
    episodeName: String? = null,
): String {
    val seasonEpisodeString = seasonEpisodeNumberToString(context, formatting, seasonNumber, episodeNumber, episodeName)
    return if (showName == null) {
        seasonEpisodeString
    } else {
        context.getString(R.string.show_dash_x, showName, seasonEpisodeString)
    }
}
