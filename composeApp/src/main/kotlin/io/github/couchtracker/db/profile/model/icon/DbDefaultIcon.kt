package io.github.couchtracker.db.profile.model.icon

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.couchtracker.R
import io.github.couchtracker.utils.Icon
import io.github.couchtracker.utils.toIcon

/**
 * Enum class of all icons supported by default by this app.
 *
 * @property id the ID of this icon that will be used in its serialization.
 */
enum class DbDefaultIcon(val icon: Icon) {

    BUS(Icons.Default.DirectionsBus),
    CAR(Icons.Default.DirectionsCar),
    CINEMA(R.drawable.screen_users),
    DISC(R.drawable.disc),
    FRIENDS(Icons.Default.Diversity1),
    HOUSE(Icons.Default.Home),
    LIVE_TV(Icons.Default.LiveTv),
    OFFICE_BUILDING(Icons.Default.CorporateFare),
    PLANE(Icons.Default.Flight),
    SCHOOL(Icons.Default.School),
    STREAM(R.drawable.stream),
    TRAIN(Icons.Default.Train),
    VACATION(Icons.Default.BeachAccess),
    VIDEO_FILE(Icons.Default.VideoFile),

    // Logos
    LOGO_APPLE_TV_PLUS(R.drawable.logo_apple_tv_plus),
    LOGO_BETAMAX(R.drawable.logo_betamax),
    LOGO_BLURAY(R.drawable.logo_bluray),
    LOGO_DVD(R.drawable.logo_dvd),
    LOGO_CRUNCHYROLL(R.drawable.logo_crunchyroll),
    LOGO_DISCOVERY_PLUS(R.drawable.logo_discovery_plus),
    LOGO_DISNEY_PLUS(R.drawable.logo_disney_plus),
    LOGO_HULU(R.drawable.logo_hulu),
    LOGO_MAX(R.drawable.logo_max),
    LOGO_NETFLIX(R.drawable.logo_netflix),
    LOGO_PARAMOUNT_PLUS(R.drawable.logo_paramount_plus),
    LOGO_PRIME_VIDEO(R.drawable.logo_prime_video),
    LOGO_VHS(R.drawable.logo_vhs),
    ;

    val id = name.lowercase().replace('_', '-')

    constructor(vector: ImageVector) : this(vector.toIcon())
    constructor(@DrawableRes resource: Int) : this(Icon.Resource(resource))

    fun toDbIcon() = DbIcon.Default(this)
}
