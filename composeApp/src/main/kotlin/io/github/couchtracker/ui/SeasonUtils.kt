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
