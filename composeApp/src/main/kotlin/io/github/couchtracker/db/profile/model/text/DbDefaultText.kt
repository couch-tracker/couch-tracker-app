package io.github.couchtracker.db.profile.model.text

import androidx.annotation.StringRes
import io.github.couchtracker.R
import io.github.couchtracker.utils.Text

/**
 * Enum class of all available texts supported by default by this app.
 *
 * @property id the ID of this text that will be used in its serialization.
 */
enum class DbDefaultText(val text: Text) {

    DEVICE_TYPE(R.string.device_type),
    DEVICE_TYPE_LAPTOP(R.string.device_type_laptop),
    DEVICE_TYPE_MONITOR(R.string.device_type_monitor),
    DEVICE_TYPE_PHONE(R.string.device_type_phone),
    DEVICE_TYPE_PROJECTOR(R.string.device_type_projector),
    DEVICE_TYPE_TABLET(R.string.device_type_tablet),
    DEVICE_TYPE_TV(R.string.device_type_tv),
    DEVICE_TYPE_VR(R.string.device_type_vr),

    MOVIE_VERSION(R.string.movie_version),
    MOVIE_VERSION_DIRECTORS_CUT(R.string.movie_version_directors_cut),
    MOVIE_VERSION_EXTENDED_CUT(R.string.movie_version_extended_cut),
    MOVIE_VERSION_THEATRICAL(R.string.movie_version_theatrical),

    PHYSICAL_MEDIA_TYPE(R.string.physical_media_type),
    PHYSICAL_MEDIA_TYPE_BETAMAX(R.string.physical_media_type_betamax),
    PHYSICAL_MEDIA_TYPE_BLURAY(R.string.physical_media_type_bluray),
    PHYSICAL_MEDIA_TYPE_DVD(R.string.physical_media_type_dvd),
    PHYSICAL_MEDIA_TYPE_VHS(R.string.physical_media_type_vhs),

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

    RESOLUTION(R.string.resolution),
    RESOLUTION_4K(R.string.resolution_4k),
    RESOLUTION_8K(R.string.resolution_8k),
    RESOLUTION_FHD(R.string.resolution_fhd),
    RESOLUTION_HD(R.string.resolution_hd),
    RESOLUTION_SD(R.string.resolution_sd),

    SOURCE(R.string.source),
    SOURCE_CINEMA(R.string.source_cinema),
    SOURCE_STREAMING(R.string.source_streaming),
    SOURCE_TV(R.string.source_tv),
    SOURCE_PHYSICAL_MEDIA(R.string.source_physical_media),
    SOURCE_DIGITAL_MEDIA(R.string.source_digital_media),

    STREAMING_PROVIDER(R.string.streaming_provider),
    STREAMING_PROVIDER_APPLE_TV_PLUS(R.string.streaming_provider_apple_tv_plus),
    STREAMING_PROVIDER_CRUNCHYROLL(R.string.streaming_provider_crunchyroll),
    STREAMING_PROVIDER_DISCOVERY_PLUS(R.string.streaming_provider_discovery_plus),
    STREAMING_PROVIDER_DISNEY_PLUS(R.string.streaming_provider_disney_plus),
    STREAMING_PROVIDER_HULU(R.string.streaming_provider_hulu),
    STREAMING_PROVIDER_MAX(R.string.streaming_provider_max),
    STREAMING_PROVIDER_NETFLIX(R.string.streaming_provider_netflix),
    STREAMING_PROVIDER_PARAMOUNT_PLUS(R.string.streaming_provider_paramount_plus),
    STREAMING_PROVIDER_PRIME_VIDEO(R.string.streaming_provider_prime_video),

    NOTES(R.string.notes),
    ;

    val id = name.lowercase().replace('_', '-')

    constructor(@StringRes resource: Int) : this(Text.Resource(resource))

    fun toDbText() = DbText.Default(this)
}
