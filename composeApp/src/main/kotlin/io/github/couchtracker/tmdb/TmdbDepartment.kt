package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.TmdbDepartment
import io.github.couchtracker.R
import io.github.couchtracker.utils.Text

fun TmdbDepartment.title(): Text {
    val resource = when (this) {
        TmdbDepartment.ACTING -> R.string.crew_departments_acting
        TmdbDepartment.ACTORS -> R.string.crew_departments_actors
        TmdbDepartment.ART -> R.string.crew_departments_art
        TmdbDepartment.CAMERA -> R.string.crew_departments_camera
        TmdbDepartment.COSTUME_AND_MAKEUP -> R.string.crew_departments_costume_and_make_up
        TmdbDepartment.CREATOR -> R.string.crew_departments_creator
        TmdbDepartment.CREW -> R.string.crew_departments_crew
        TmdbDepartment.DIRECTING -> R.string.crew_departments_directing
        TmdbDepartment.EDITING -> R.string.crew_departments_editing
        TmdbDepartment.LIGHTING -> R.string.crew_departments_lighting
        TmdbDepartment.PRODUCTION -> R.string.crew_departments_production
        TmdbDepartment.SOUND -> R.string.crew_departments_sound
        TmdbDepartment.VISUAL_EFFECTS -> R.string.crew_departments_visual_effects
        TmdbDepartment.WRITING -> R.string.crew_departments_writing
    }
    return Text.Resource(resource)
}
