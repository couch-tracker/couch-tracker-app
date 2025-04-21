package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.couchtracker.R
import io.github.couchtracker.db.user.model.partialtime.PartialDateTime
import io.github.couchtracker.intl.datetime.Skeletons
import io.github.couchtracker.intl.datetime.localized
import io.github.couchtracker.utils.str
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private enum class DatePrecision {
    YEAR,
    MONTH,
    DATE,
}

enum class DatePickerWorkflowStep {
    CHOOSE_DATE,
    CHOOSE_TIME,
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun PartialDateTimePickerDialog(
    initialDate: PartialDateTime.Local?,
    workflowStep: DatePickerWorkflowStep,
    setWorkflowStep: (DatePickerWorkflowStep) -> Unit,
    onDateSelected: (PartialDateTime.Local) -> Unit,
    close: () -> Unit,
) {
    var precision by rememberDatePrecision(initialDate)
    val datePickerState = rememberDatePickerState(initialDate)
    val timePickerState = rememberTimePickerState(initialDate)

    DatePickerDialog(
        onDismissRequest = close,
        confirmButton = {
            val selectedDate = selectedPartialDate(precision, datePickerState)
            when (workflowStep) {
                DatePickerWorkflowStep.CHOOSE_DATE -> when (precision) {
                    DatePrecision.YEAR, DatePrecision.MONTH -> DialogButtonForDate(selectedDate, onDateSelected, close)
                    DatePrecision.DATE -> {
                        TextButton(
                            enabled = selectedDate != null,
                            onClick = { setWorkflowStep(DatePickerWorkflowStep.CHOOSE_TIME) },
                        ) {
                            Text(R.string.continue_action.str())
                        }
                    }
                }

                DatePickerWorkflowStep.CHOOSE_TIME -> {
                    DialogButtonForDate(selectedDate, onDateSelected, close, label = R.string.skip_action.str())
                    val selectedTime = selectedPartialDateTime(precision, datePickerState, timePickerState)
                    DialogButtonForDate(selectedTime, onDateSelected, close)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = close) {
                Text(R.string.cancel_action.str())
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val selectedDate = selectedPartialDate(precision, datePickerState) as? PartialDateTime.Local.Date
            if (workflowStep == DatePickerWorkflowStep.CHOOSE_TIME && selectedDate != null) {
                TimeSelectorTopAppBar(selectedDate, setWorkflowStep)
                TimePicker(state = timePickerState)
            } else {
                DatePicker(
                    state = datePickerState,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    colors = if (precision == DatePrecision.DATE) {
                        DatePickerDefaults.colors()
                    } else {
                        DatePickerDefaults.colors(
                            selectedDayContainerColor = Color.Transparent,
                            selectedDayContentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                )
                DatePrecisionSelector(precision) {
                    datePickerState.selectedDateMillis = null
                    precision = it
                }
            }
        }
    }
}

/** The [TopAppBar] to show on the time picker */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectorTopAppBar(
    selectedDate: PartialDateTime.Local.Date,
    openTimePicker: (DatePickerWorkflowStep) -> Unit,
) {
    TopAppBar(
        title = {
            Text(selectedDate.localized(Skeletons.LONG_DATE).string())
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        navigationIcon = {
            IconButton({ openTimePicker(DatePickerWorkflowStep.CHOOSE_DATE) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = R.string.back_action.str(),
                )
            }
        },
    )
}

/** The button bar to select the time precision */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePrecisionSelector(precision: DatePrecision, selectPrecision: (DatePrecision) -> Unit) {
    @Composable
    fun SingleChoiceSegmentedButtonRowScope.SegmentedButton(index: Int, p: DatePrecision, label: String) {
        SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
            selected = precision == p,
            onClick = { selectPrecision(p) },
            label = { Text(label, maxLines = 1) },
        )
    }
    Text(R.string.date_and_time_dialog_precision.str())
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(0, DatePrecision.YEAR, R.string.date_and_time_precision_year.str())
        SegmentedButton(1, DatePrecision.MONTH, R.string.date_and_time_precision_month.str())
        SegmentedButton(2, DatePrecision.DATE, R.string.date_and_time_precision_date.str())
    }
}

/** A dialog-button to select the given [dateTime] */
@Composable
private fun DialogButtonForDate(
    @Suppress("CanBeNonNullable") // False positive, see https://github.com/detekt/detekt/issues/7420
    dateTime: PartialDateTime.Local?,
    onDateSelected: (PartialDateTime.Local) -> Unit,
    close: () -> Unit,
    label: String = android.R.string.ok.str(),
) {
    TextButton(
        enabled = dateTime != null,
        onClick = {
            checkNotNull(dateTime)
            onDateSelected(dateTime)
            close()
        },
    ) {
        Text(label)
    }
}

/** Given the initial date, creates and remembers a [DatePrecision] */
@Composable
private fun rememberDatePrecision(initialDate: PartialDateTime.Local?): MutableState<DatePrecision> {
    return remember {
        mutableStateOf(
            when (initialDate) {
                is PartialDateTime.Local.Year -> DatePrecision.YEAR
                is PartialDateTime.Local.YearMonth -> DatePrecision.MONTH
                is PartialDateTime.Local.Date, is PartialDateTime.Local.DateTime, null -> DatePrecision.DATE
            },
        )
    }
}

/** Given the initial date, creates and remembers a [DatePickerState] */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberDatePickerState(initialDate: PartialDateTime.Local?): DatePickerState {
    val initialEpochMillis = initialDate?.toInstant(TimeZone.UTC)?.toEpochMilliseconds()
    return rememberDatePickerState(
        initialSelectedDateMillis = when (initialDate) {
            null -> null
            is PartialDateTime.Local.Year -> null
            is PartialDateTime.Local.YearMonth -> null
            is PartialDateTime.Local.Date -> initialEpochMillis
            is PartialDateTime.Local.DateTime -> initialEpochMillis
        },
        initialDisplayedMonthMillis = initialEpochMillis,
    )
}

/** Given the initial date, creates and remembers a [TimePickerState] */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberTimePickerState(initialDate: PartialDateTime.Local?): TimePickerState {
    val initialDateTime = if (initialDate is PartialDateTime.Local.DateTime) {
        initialDate.dateTime
    } else {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }
    return rememberTimePickerState(
        initialHour = initialDateTime.hour,
        initialMinute = initialDateTime.minute,
    )
}

/** The currently selected [LocalDate] */
@OptIn(ExperimentalMaterial3Api::class)
private fun selectedLocalDate(
    datePrecision: DatePrecision,
    datePickerState: DatePickerState,
): LocalDate? {
    fun millisToDate(millis: Long): LocalDate {
        return Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
    }
    return when (datePrecision) {
        DatePrecision.YEAR, DatePrecision.MONTH -> millisToDate(datePickerState.displayedMonthMillis)
        DatePrecision.DATE -> {
            datePickerState.selectedDateMillis?.let { millis ->
                millisToDate(millis)
            }
        }
    }
}

/** The current selected partial date, with a precision that ranges from year to date */
@OptIn(ExperimentalMaterial3Api::class)
private fun selectedPartialDate(
    datePrecision: DatePrecision,
    datePickerState: DatePickerState,
): PartialDateTime.Local? {
    val localDate = selectedLocalDate(datePrecision, datePickerState) ?: return null
    return when (datePrecision) {
        DatePrecision.YEAR -> PartialDateTime.Local.Year(localDate.year)
        DatePrecision.MONTH -> PartialDateTime.Local.YearMonth(localDate.year, localDate.month)
        DatePrecision.DATE -> PartialDateTime.Local.Date(localDate)
    }
}

/** The current selected DateTime, or null if a date/time hasn't been chosen, or the precision is year/month */
@OptIn(ExperimentalMaterial3Api::class)
private fun selectedPartialDateTime(
    datePrecision: DatePrecision,
    datePickerState: DatePickerState,
    timePickerState: TimePickerState,
): PartialDateTime.Local.DateTime? {
    return when (datePrecision) {
        DatePrecision.YEAR, DatePrecision.MONTH -> null
        DatePrecision.DATE -> {
            selectedLocalDate(datePrecision, datePickerState)?.let { date ->
                val time = LocalTime(timePickerState.hour, timePickerState.minute)
                PartialDateTime.Local.DateTime(LocalDateTime(date, time))
            }
        }
    }
}
