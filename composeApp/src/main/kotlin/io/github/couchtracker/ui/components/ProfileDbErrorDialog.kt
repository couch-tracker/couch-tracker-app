package io.github.couchtracker.ui.components

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.intl.errorMessage
import io.github.couchtracker.utils.ProfileDbActionState
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch

@Composable
fun ProfileDbErrorDialog(actionState: ProfileDbActionState<*>) {
    val state = actionState.current

    if (state is Result.Error) {
        val detailedMessage = state.error.errorMessage()
        val exception = if (state.error is ProfileDbError.WithException) state.error.exception else null
        AlertDialog(
            icon = { Icon(Icons.Default.Error, contentDescription = null) },
            title = { Text(R.string.save_failed.str()) },
            text = {
                Column {
                    Text(R.string.save_failed_message.str(detailedMessage))

                    if (exception != null) {
                        Spacer(Modifier.size(8.dp))
                        ExceptionStackTrace(exception)
                    }
                }
            },
            onDismissRequest = { actionState.reset() },
            confirmButton = {
                TextButton(
                    onClick = { actionState.reset() },
                    content = { Text(android.R.string.ok.str()) },
                )
            },
        )
    }
}

@Composable
private fun ColumnScope.ExceptionStackTrace(exception: Exception) {
    val coroutineScope = rememberCoroutineScope()
    var exceptionVisible by remember { mutableStateOf(false) }
    Row(Modifier.clickable { exceptionVisible = !exceptionVisible }) {
        val (icon, textRes) = if (exceptionVisible) {
            Icons.AutoMirrored.Default.ArrowLeft to R.string.hide_exception
        } else {
            Icons.AutoMirrored.Default.ArrowRight to R.string.show_exception
        }
        Icon(icon, contentDescription = null)
        Text(textRes.str(), textDecoration = TextDecoration.Underline)
    }
    if (exceptionVisible) {
        val clipboard = LocalClipboard.current
        val stacktrace = exception.stackTraceToString()

        SelectionContainer(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            content = { Text(stacktrace, style = MaterialTheme.typography.bodySmall) },
        )
        Text(
            modifier = Modifier.clickable {
                coroutineScope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("", stacktrace)))
                }
            },
            text = R.string.copy_exception_stacktrace.str(),
            textDecoration = TextDecoration.Underline,
        )
    }
}
