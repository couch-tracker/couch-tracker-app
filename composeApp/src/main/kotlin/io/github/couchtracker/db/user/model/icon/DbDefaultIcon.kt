package io.github.couchtracker.db.user.model.icon

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Diversity1
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.House
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
    HOUSE(Icons.Default.House),
    LIVE_TV(Icons.Default.LiveTv),
    OFFICE_BUILDING(Icons.Default.CorporateFare),
    PLANE(Icons.Default.Flight),
    SCHOOL(Icons.Default.School),
    STREAM(R.drawable.stream),
    TRAIN(Icons.Default.Train),
    VACATION(Icons.Default.BeachAccess),
    VIDEO_FILE(Icons.Default.VideoFile),
    ;

    val id = name.lowercase().replace('_', '-')

    constructor(vector: ImageVector) : this(vector.toIcon())
    constructor(@DrawableRes resource: Int) : this(Icon.Resource(resource))

    fun toDbIcon() = DbIcon.Default(this)
}
