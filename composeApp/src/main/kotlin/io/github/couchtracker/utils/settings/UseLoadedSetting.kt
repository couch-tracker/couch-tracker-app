package io.github.couchtracker.utils.settings

@RequiresOptIn(
    message = "This setting API shouldn't be used directly, as it will trigger a reload of the settings. " +
        "Use an already loaded version available either via compose or via dependency injection",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class UseLoadedSetting
