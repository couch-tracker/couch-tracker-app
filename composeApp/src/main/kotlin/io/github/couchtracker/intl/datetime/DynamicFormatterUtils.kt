package io.github.couchtracker.intl.datetime

import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.withNextTickAtMost
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

fun chooseThreshold(
    now: Instant,
    thresholdStart: Instant,
    thresholdEnd: Instant,
    withinThreshold: () -> TickingValue<String>,
    outsideThreshold: () -> TickingValue<String>,
): TickingValue<String> {
    return if (now <= thresholdStart) {
        outsideThreshold().withNextTickAtMost(thresholdStart - now + 1.nanoseconds)
    } else if (now < thresholdEnd) {
        withinThreshold().withNextTickAtMost(thresholdEnd - now)
    } else {
        outsideThreshold()
    }
}
