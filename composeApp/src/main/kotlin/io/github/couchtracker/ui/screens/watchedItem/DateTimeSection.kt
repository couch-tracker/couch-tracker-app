package io.github.couchtracker.ui.screens.watchedItem

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemDimensionSelectionValidity
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.intl.datetime.MonthSkeleton
import io.github.couchtracker.intl.datetime.Skeletons
import io.github.couchtracker.intl.datetime.TimeSkeleton
import io.github.couchtracker.intl.datetime.YearSkeleton
import io.github.couchtracker.intl.datetime.localized
import io.github.couchtracker.ui.StartSpaceLast
import io.github.couchtracker.ui.components.DatePickerWorkflowStep
import io.github.couchtracker.ui.components.PartialDateTimePickerDialog
import io.github.couchtracker.ui.components.TimezonePickerDialog
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState.CustomDateDialogVisibility.DATE
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState.CustomDateDialogVisibility.HIDDEN
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState.CustomDateDialogVisibility.TIME
import io.github.couchtracker.ui.screens.watchedItem.DateTimeSectionState.CustomDateDialogVisibility.TIMEZONE
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.roundToSeconds
import io.github.couchtracker.utils.str
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

class DateTimeSectionState(val initial: PartialDateTime? = null) {

    var dateTime by mutableStateOf(initial?.let { DateAndTimeValue(DateAndTimeSectionChoices.Custom, it) })
    var customDateDialogVisibility by mutableStateOf(HIDDEN)
    var customTimezone by mutableStateOf<TimeZone?>(TimeZone.currentSystemDefault())

    fun setCustomDate(
        localDateTime: PartialDateTime.Local? = dateTime?.dateTime?.local,
        timeZone: TimeZone? = customTimezone,
    ) {
        customTimezone = timeZone
        dateTime = if (localDateTime == null) {
            null
        } else {
            DateAndTimeValue(
                DateAndTimeSectionChoices.Custom,
                if (timeZone == null) {
                    localDateTime
                } else {
                    PartialDateTime.Zoned(localDateTime, timeZone)
                },
            )
        }
    }

    enum class CustomDateDialogVisibility {
        HIDDEN,
        DATE,
        TIME,
        TIMEZONE,
    }
}

data class DateAndTimeValue(
    val category: DateAndTimeSectionChoices,
    val dateTime: PartialDateTime,
)

sealed interface DateAndTimeSectionChoices {

    val displayName: Text

    sealed interface Preset : DateAndTimeSectionChoices {

        fun default(
            runtime: Duration,
            now: Instant = Clock.System.now(),
            timezone: TimeZone = TimeZone.currentSystemDefault(),
        ): PartialDateTime

        data class JustStarted(override val displayName: Text) : Preset {
            override fun default(runtime: Duration, now: Instant, timezone: TimeZone) = PartialDateTime.Zoned(
                local = PartialDateTime.Local.DateTime(now.toLocalDateTime(timezone).roundToSeconds()),
                zone = timezone,
            )
        }

        data class JustFinished(override val displayName: Text) : Preset {
            override fun default(runtime: Duration, now: Instant, timezone: TimeZone) = PartialDateTime.Zoned(
                local = PartialDateTime.Local.DateTime(now.minus(runtime).toLocalDateTime(timezone).roundToSeconds()),
                zone = timezone,
            )
        }

        data class Today(override val displayName: Text) : Preset {
            override fun default(runtime: Duration, now: Instant, timezone: TimeZone) = PartialDateTime.Zoned(
                local = PartialDateTime.Local.Date(now.toLocalDateTime(timezone).date),
                zone = timezone,
            )
        }
    }

    data object Custom : DateAndTimeSectionChoices {
        override val displayName = Text.Resource(R.string.watched_item_time_custom)
    }

    companion object {

        fun ofWatchedItemType(type: WatchedItemType): List<DateAndTimeSectionChoices> {
            return when (type) {
                WatchedItemType.MOVIE -> listOf(
                    Preset.JustStarted(Text.Resource(R.string.watched_movie_time_just_started)),
                    Preset.JustFinished(Text.Resource(R.string.watched_movie_time_just_finished)),
                    Preset.Today(Text.Resource(R.string.watched_item_time_today)),
                    Custom,
                )

                WatchedItemType.EPISODE -> TODO()
            }
        }
    }
}

@Composable
fun DateTimeSectionDialog(sectionState: DateTimeSectionState) {
    when (val visibility = sectionState.customDateDialogVisibility) {
        DATE, TIME -> {
            PartialDateTimePickerDialog(
                initialDate = sectionState.dateTime?.dateTime?.local,
                workflowStep = if (visibility == TIME) DatePickerWorkflowStep.CHOOSE_TIME else DatePickerWorkflowStep.CHOOSE_DATE,
                onDateSelected = { date ->
                    sectionState.setCustomDate(date)
                },
                setWorkflowStep = { step ->
                    sectionState.customDateDialogVisibility = when (step) {
                        DatePickerWorkflowStep.CHOOSE_TIME -> TIME
                        DatePickerWorkflowStep.CHOOSE_DATE -> DATE
                    }
                },
                close = {
                    sectionState.customDateDialogVisibility = HIDDEN
                },
            )
        }

        TIMEZONE -> {
            TimezonePickerDialog(
                timezone = sectionState.customTimezone,
                onTimezoneSelected = {
                    sectionState.setCustomDate(timeZone = it)
                },
                onClose = {
                    sectionState.customDateDialogVisibility = HIDDEN
                },
            )
        }

        HIDDEN -> {}
    }
}

@Composable
@OptIn(ExperimentalAnimationApi::class)
fun WatchedItemSheetScope.DateTimeSection(
    enabled: Boolean,
    sectionState: DateTimeSectionState,
    watchedItemType: WatchedItemType,
    mediaRuntime: Duration?,
) {
    Section(title = Text.Resource(R.string.date_and_time), validity = WatchedItemDimensionSelectionValidity.Valid) {
        val transition = updateTransition(sectionState.dateTime)
        transition.Crossfade(
            contentKey = { dt ->
                dt?.category == DateAndTimeSectionChoices.Custom
            },
        ) { dt ->
            if (dt?.category == DateAndTimeSectionChoices.Custom) {
                CustomDateTimeRow(
                    enabled = enabled,
                    selectedDateTime = dt.dateTime,
                    setCustomDialogVisibility = { sectionState.customDateDialogVisibility = it },
                    deselect = { sectionState.dateTime = null },
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(DateAndTimeSectionChoices.ofWatchedItemType(watchedItemType)) { category ->
                        FilterChip(
                            enabled = enabled,
                            selected = category == dt?.category,
                            onClick = {
                                when (category) {
                                    dt?.category -> sectionState.dateTime = null
                                    DateAndTimeSectionChoices.Custom -> sectionState.customDateDialogVisibility = DATE
                                    is DateAndTimeSectionChoices.Preset -> sectionState.dateTime = DateAndTimeValue(
                                        category = category,
                                        dateTime = category.default(runtime = mediaRuntime ?: watchedItemType.fallbackRuntime),
                                    )
                                }
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                            label = { Text(category.displayName.string()) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomDateTimeRow(
    enabled: Boolean,
    selectedDateTime: PartialDateTime,
    setCustomDialogVisibility: (DateTimeSectionState.CustomDateDialogVisibility) -> Unit,
    deselect: () -> Unit,
) {
    val localDate = selectedDateTime.local
    val dateString = when (localDate) {
        is PartialDateTime.Local.Year -> localDate.localized(YearSkeleton.NUMERIC).string()
        is PartialDateTime.Local.YearMonth -> localDate.localized(YearSkeleton.NUMERIC, MonthSkeleton.WIDE).string()
        is PartialDateTime.Local.Date -> localDate.localized(Skeletons.MEDIUM_DATE).string()
        is PartialDateTime.Local.DateTime -> localDate.localized(Skeletons.MEDIUM_DATE).string()
    }
    val timeString = if (localDate is PartialDateTime.Local.DateTime) {
        localDate.localized(TimeSkeleton.MINUTES).string()
    } else {
        null
    }
    val tzString = if (selectedDateTime is PartialDateTime.Zoned) {
        selectedDateTime.zone.id
    } else {
        null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.StartSpaceLast,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.padding(horizontal = 4.dp)) {
            DateTimePiece(
                enabled = enabled,
                dialogToOpen = DATE,
                setCustomDialogVisibility = setCustomDialogVisibility,
                text = dateString,
                selected = true,
                primary = true,
                roundEnd = timeString == null,
            )
            if (timeString != null) {
                DateTimePiece(
                    enabled = enabled,
                    dialogToOpen = TIME,
                    setCustomDialogVisibility = setCustomDialogVisibility,
                    text = timeString,
                    selected = true,
                    primary = false,
                    roundStart = false,
                )
            }
        }
        Row(Modifier.padding(horizontal = 4.dp)) {
            DateTimePiece(
                enabled = enabled,
                dialogToOpen = TIMEZONE,
                setCustomDialogVisibility = setCustomDialogVisibility,
                text = tzString ?: R.string.no_timezone.str(),
                selected = tzString != null,
                primary = false,
                icon = Icons.Default.Public,
            )
        }
        IconButton(enabled = enabled, onClick = deselect) {
            Icon(Icons.Default.Close, R.string.remove_action.str())
        }
    }
}

@Composable
private fun DateTimePiece(
    enabled: Boolean,
    dialogToOpen: DateTimeSectionState.CustomDateDialogVisibility,
    setCustomDialogVisibility: (DateTimeSectionState.CustomDateDialogVisibility) -> Unit,
    text: String,
    selected: Boolean,
    primary: Boolean,
    icon: ImageVector? = null,
    roundStart: Boolean = true,
    roundEnd: Boolean = true,
) {
    var shape = MaterialTheme.shapes.small
    if (!roundStart) {
        shape = shape.copy(topStart = ZeroCornerSize, bottomStart = ZeroCornerSize)
    }
    if (!roundEnd) {
        shape = shape.copy(topEnd = ZeroCornerSize, bottomEnd = ZeroCornerSize)
    }
    InputChip(
        enabled = enabled,
        selected = selected,
        onClick = { setCustomDialogVisibility(dialogToOpen) },
        label = { Text(text) },
        colors = InputChipDefaults.inputChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (primary) 1f else 0.5f),
        ),
        leadingIcon = icon?.let {
            {
                Icon(icon, null)
            }
        },
        border = null,
        shape = shape,
    )
}
