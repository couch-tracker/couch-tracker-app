package io.github.couchtracker.db.user.model.text

import androidx.annotation.StringRes
import io.github.couchtracker.R
import io.github.couchtracker.utils.Text

/**
 * Enum class of all available texts supported by default by this app.
 *
 * @property id the ID of this text that will be used in its serialization.
 */
enum class DbDefaultText(val text: Text) {

    PLACE(R.string.place),
    PLACE_BUS(R.string.place_bus),
    PLACE_CAR(R.string.place_car),
    PLACE_CINEMA(R.string.place_cinema),
    PLACE_FRIENDS_HOUSE(R.string.place_friends_house),
    PLACE_HOME(R.string.place_home),
    PLACE_PLANE(R.string.place_plane),
    PLACE_SCHOOL(R.string.place_school),
    PLACE_TRAIN(R.string.place_train),
    PLACE_VACATION(R.string.place_vacation),
    PLACE_WORK(R.string.place_work),

    SOURCE(R.string.source),
    SOURCE_CINEMA(R.string.source_cinema),
    SOURCE_STREAMING(R.string.source_streaming),
    SOURCE_TV(R.string.source_tv),
    SOURCE_PHYSICAL_MEDIA(R.string.source_physical_media),
    SOURCE_DIGITAL_MEDIA(R.string.source_digital_media),
    ;

    val id = name.lowercase().replace('_', '-')

    constructor(@StringRes resource: Int) : this(Text.Resource(resource))

    fun toDbText() = DbText.Default(this)
}
