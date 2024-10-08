package io.github.couchtracker.db.user.model.icon

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
import couch_tracker_app.composeapp.generated.resources.Res
import couch_tracker_app.composeapp.generated.resources.disc
import couch_tracker_app.composeapp.generated.resources.screen_users
import couch_tracker_app.composeapp.generated.resources.stream
import io.github.couchtracker.utils.Icon
import io.github.couchtracker.utils.toIcon
import org.jetbrains.compose.resources.DrawableResource

/**
 * Enum class of all icons supported by default by this app.
 *
 * @property id the ID of this icon that will be used in its serialization.
 */
enum class DbDefaultIcon(val icon: Icon) {

    BUS(Icons.Default.DirectionsBus),
    CAR(Icons.Default.DirectionsCar),
    CINEMA(Res.drawable.screen_users),
    DISC(Res.drawable.disc),
    FRIENDS(Icons.Default.Diversity1),
    HOUSE(Icons.Default.House),
    LIVE_TV(Icons.Default.LiveTv),
    OFFICE_BUILDING(Icons.Default.CorporateFare),
    PLANE(Icons.Default.Flight),
    SCHOOL(Icons.Default.School),
    STREAM(Res.drawable.stream),
    TRAIN(Icons.Default.Train),
    VACATION(Icons.Default.BeachAccess),
    VIDEO_FILE(Icons.Default.VideoFile),
    ;

    val id = name.lowercase().replace('_', '-')

    constructor(vector: ImageVector) : this(vector.toIcon())
    constructor(resource: DrawableResource) : this(resource.toIcon())

    fun toDbIcon() = DbIcon.Default(this)
}
