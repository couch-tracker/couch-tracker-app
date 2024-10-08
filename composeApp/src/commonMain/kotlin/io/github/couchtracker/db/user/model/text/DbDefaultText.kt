package io.github.couchtracker.db.user.model.text

import couch_tracker_app.composeapp.generated.resources.Res
import couch_tracker_app.composeapp.generated.resources.place
import couch_tracker_app.composeapp.generated.resources.place_bus
import couch_tracker_app.composeapp.generated.resources.place_car
import couch_tracker_app.composeapp.generated.resources.place_cinema
import couch_tracker_app.composeapp.generated.resources.place_friends_house
import couch_tracker_app.composeapp.generated.resources.place_home
import couch_tracker_app.composeapp.generated.resources.place_plane
import couch_tracker_app.composeapp.generated.resources.place_school
import couch_tracker_app.composeapp.generated.resources.place_train
import couch_tracker_app.composeapp.generated.resources.place_vacation
import couch_tracker_app.composeapp.generated.resources.place_work
import couch_tracker_app.composeapp.generated.resources.source
import couch_tracker_app.composeapp.generated.resources.source_cinema
import couch_tracker_app.composeapp.generated.resources.source_digital_media
import couch_tracker_app.composeapp.generated.resources.source_physical_media
import couch_tracker_app.composeapp.generated.resources.source_streaming
import couch_tracker_app.composeapp.generated.resources.source_tv
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.toText
import org.jetbrains.compose.resources.StringResource

/**
 * Enum class of all available texts supported by default by this app.
 *
 * @property id the ID of this text that will be used in its serialization.
 */
enum class DbDefaultText(val text: Text) {

    PLACE(Res.string.place),
    PLACE_BUS(Res.string.place_bus),
    PLACE_CAR(Res.string.place_car),
    PLACE_CINEMA(Res.string.place_cinema),
    PLACE_FRIENDS_HOUSE(Res.string.place_friends_house),
    PLACE_HOME(Res.string.place_home),
    PLACE_PLANE(Res.string.place_plane),
    PLACE_SCHOOL(Res.string.place_school),
    PLACE_TRAIN(Res.string.place_train),
    PLACE_VACATION(Res.string.place_vacation),
    PLACE_WORK(Res.string.place_work),

    SOURCE(Res.string.source),
    SOURCE_CINEMA(Res.string.source_cinema),
    SOURCE_STREAMING(Res.string.source_streaming),
    SOURCE_TV(Res.string.source_tv),
    SOURCE_PHYSICAL_MEDIA(Res.string.source_physical_media),
    SOURCE_DIGITAL_MEDIA(Res.string.source_digital_media),
    ;

    val id = name.lowercase().replace('_', '-')

    constructor(resource: StringResource) : this(resource.toText())

    fun toDbText() = DbText.Default(this)
}
