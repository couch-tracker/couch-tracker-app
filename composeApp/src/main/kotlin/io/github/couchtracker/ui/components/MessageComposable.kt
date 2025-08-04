package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.utils.str

@Composable
fun MessageComposable(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    details: String? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, Modifier.size(40.dp))
        Spacer(Modifier.height(24.dp))
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall)
        if (details != null) {
            Spacer(Modifier.height(24.dp))
            Text(details, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        }
        content()
    }
}

@Composable
fun ErrorMessageComposable(
    errorMessage: String,
    errorDetails: String?,
    modifier: Modifier = Modifier,
    retry: (() -> Unit)? = null,
) {
    MessageComposable(
        modifier = modifier,
        icon = Icons.Filled.Error,
        message = errorMessage,
        details = errorDetails,
    ) {
        if (retry != null) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(retry) {
                Text(R.string.retry_action.str())
            }
        }
    }
}

@Composable
fun WipMessageComposable(
    gitHubIssueId: Int,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val githubUri = "https://github.com/couch-tracker/couch-tracker-app/issues/$gitHubIssueId"

    MessageComposable(
        modifier = modifier,
        icon = Icons.Filled.Construction,
        message = "Under construction",
        details = "This part of the app is still under construction. See GitHub issue below.",
    ) {
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = { uriHandler.openUri(githubUri) }) {
            Text("Open GitHub issue #$gitHubIssueId")
        }
    }
}
