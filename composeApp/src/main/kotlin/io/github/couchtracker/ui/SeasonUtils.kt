package io.github.couchtracker.ui

import android.content.Context
import app.moviebase.tmdb.model.TmdbSeason
import io.github.couchtracker.R

fun seasonNumberToString(context: Context, seasonNumber: Int): String {
    return if (seasonNumber == 0) {
        context.getString(R.string.season_specials)
    } else {
        context.getString(R.string.season_x, seasonNumber)
    }
}

fun seasonEpisodeNumberToString(context: Context, seasonNumber: Int, episodeNumber: Int, episodeName: String? = null): String {
    return if (seasonNumber == 0) {
        if (episodeName == null) {
            context.getString(R.string.season_special_episode, episodeNumber)
        } else {
            context.getString(R.string.season_special_episode_with_name, episodeNumber, episodeName)
        }
    } else {
        if (episodeName == null) {
            context.getString(R.string.season_episode, seasonNumber, episodeNumber)
        } else {
            context.getString(R.string.season_episode_with_name, seasonNumber, episodeNumber, episodeName)
        }
    }
}

fun showSeasonEpisodeNumberToString(
    context: Context,
    showName: String?,
    seasonNumber: Int,
    episodeNumber: Int,
    episodeName: String? = null,
): String {
    val seasonEpisodeString = seasonEpisodeNumberToString(context, seasonNumber, episodeNumber, episodeName)
    return if (showName == null) {
        seasonEpisodeString
    } else {
        context.getString(R.string.show_dash_x, showName, seasonEpisodeString)
    }
}

data class SeasonNames(
    val mainName: String,
    val secondaryName: String?,
)

fun TmdbSeason.names(context: Context): SeasonNames {
    val seasonName = name
    val defaultName = seasonNumberToString(context, seasonNumber)
    if (seasonName == null) {
        return SeasonNames(
            mainName = defaultName,
            secondaryName = null,
        )
    }
    // See https://www.themoviedb.org/bible/tv/59f73eb49251416e71000026#59f7445c9251416e7100003b
    val isWorthDisplayingAltSeasonName = !seasonName.equals(defaultName, ignoreCase = true) && seasonName != "Series $seasonNumber"
    return SeasonNames(
        mainName = seasonName,
        secondaryName = if (isWorthDisplayingAltSeasonName) defaultName else null,
    )
}
