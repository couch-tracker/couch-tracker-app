package io.github.couchtracker.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Returns the part of this duration of the given [unit].
 *
 * For example:
 * - `1h30m`, `MINUTES` -> `30`
 * - `30m`, `HOURS` -> `0`
 * - `-50m44s`, `SECONDS` -> `-44`
 */
@Suppress("MagicNumber")
fun Duration.unitPart(unit: DurationUnit): Long {
    return toComponents { days, hours, minutes, seconds, nanoseconds ->
        when (unit) {
            DurationUnit.NANOSECONDS -> (nanoseconds % 1_000).toLong()
            DurationUnit.MICROSECONDS -> ((nanoseconds / 1000) % 1000).toLong()
            DurationUnit.MILLISECONDS -> nanoseconds / 1_000_000L
            DurationUnit.SECONDS -> seconds.toLong()
            DurationUnit.MINUTES -> minutes.toLong()
            DurationUnit.HOURS -> hours.toLong()
            DurationUnit.DAYS -> days
        }
    }
}

/**
 * Returns the amount of time that needs to pass for this duration's [unit] to change, assuming the duration moves backwards, i.e. a
 * positive duration means something that will happen in the future, and a negative duration something that has happened in the past.
 *
 * If the duration would change sign before the unit, that is returned instead (see last example).
 *
 * For example:
 * - `-1h20m`, `HOURS` -> `40m`
 * - `-1h20m`, `MINUTES` -> `1m`
 * - `1h20m`, `HOURS` -> `20m1ns`
 * - `1h20m`, `MINUTES` -> `1ns`
 * - `20m`, `HOURS` -> `20m`
 */
fun Duration.remainderUntilNextUnitBoundary(unit: DurationUnit): Duration {
    val durationNanos = this.absoluteValue.inWholeNanoseconds
    val unitNanos = 1.toDuration(unit).inWholeNanoseconds
    val remainderNs = durationNanos % unitNanos
    return if (isPositive()) {
        minOf(this, (remainderNs + 1).nanoseconds)
    } else {
        (unitNanos - remainderNs).nanoseconds
    }
}
