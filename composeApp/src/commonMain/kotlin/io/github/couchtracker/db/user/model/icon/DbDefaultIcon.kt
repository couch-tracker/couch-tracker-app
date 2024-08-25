package io.github.couchtracker.db.user.model.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CorporateFare
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.House
import couch_tracker_app.composeapp.generated.resources.Res
import couch_tracker_app.composeapp.generated.resources.screen_users
import io.github.couchtracker.utils.Icon
import io.github.couchtracker.utils.toIcon

/**
 * Enum class of all icons supported by default by this app.
 *
 * @property id the ID of this icon that will be used in its serialization.
 */
enum class DbDefaultIcon(val icon: Icon) {

    HOME(Icons.Default.House.toIcon()),
    OFFICE_BUILDING(Icons.Default.CorporateFare.toIcon()),
    PLANE(Icons.Default.Flight.toIcon()),
    CINEMA(Res.drawable.screen_users.toIcon()),
    ;

    val id = name.lowercase().replace('_', '-')

    fun toDbIcon() = DbIcon.Default(this)
}
