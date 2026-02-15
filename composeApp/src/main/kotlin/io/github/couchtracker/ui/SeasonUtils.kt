package io.github.couchtracker.ui

import android.content.Context
import io.github.couchtracker.R

fun seasonNumberToString(context: Context, seasonNumber: Int): String {
    return if (seasonNumber == 0) {
        context.getString(R.string.season_specials)
    } else {
        context.getString(R.string.season_x, seasonNumber)
    }
}

fun isWorthDisplayingAltSeasonName(seasonName: String, seasonNumber: Int, altSeasonName: String): Boolean {
    // See https://www.themoviedb.org/bible/tv/59f73eb49251416e71000026#59f7445c9251416e7100003b
    return !seasonName.equals(altSeasonName, ignoreCase = true) && seasonName != "Series $seasonNumber"
}

/**
 * The string to show below the season name
 */
fun seasonNameSubtitle(context: Context, displayAltName: Boolean, showName: String?, altSeasonName: String): String? {
    return if (displayAltName) {
        if (showName != null) {
            context.getString(R.string.show_dash_season, showName, altSeasonName)
        } else {
            altSeasonName
        }
    } else {
        showName
    }
}
