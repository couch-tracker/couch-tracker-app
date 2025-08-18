package io.github.couchtracker.utils.settings

import androidx.datastore.preferences.core.Preferences

/**
 * Base class that can be implemented by containers of [Setting] instances.
 *
 * It offers a way to declare [Setting] constants and automatically accumulate them in a [Map] that can be later retrieved with [settings].
 */
abstract class AbstractSettings {

    private val _settings = mutableMapOf<Preferences.Key<*>, Setting<*, *, *, *>>()

    private val lazySettings = lazy { _settings.toMap() }

    /**
     * Lazy map that returns all the [Setting] declared in this object.
     *
     * Once called, no new settings can be registered in this instance.
     */
    val settings by lazySettings

    private fun requireNotInitialized() {
        require(!lazySettings.isInitialized()) { "Cannot add settings after lazy settings has initialized" }
    }

    /**
     * Registers the given [setting] in this instance.
     *
     * Cannot be called after [AbstractSettings.settings] has been obtained the first time.
     */
    protected fun <S : Setting<*, *, *, *>> setting(setting: S): S {
        requireNotInitialized()
        _settings[setting.key] = setting
        return setting
    }

    /**
     * Registers all the given [settings] in this instance.
     *
     * Cannot be called after [AbstractSettings.settings] has been obtained the first time.
     *
     * This is useful to logically separate different settings in various [AbstractSettings] instances and accumulate them all on a single
     * root [AbstractSettings] instance.
     */
    protected fun <S : AbstractSettings> settings(settings: S): S {
        requireNotInitialized()
        _settings.putAll(settings.settings)
        return settings
    }
}
