package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.utils.str

@Composable
fun ErrorMessageComposable(
    modifier: Modifier = Modifier,
    errorMessage: String,
    retry: (() -> Unit)? = null,
) {
    Column(
        modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Error, contentDescription = null, Modifier.size(40.dp))
        Spacer(Modifier.height(24.dp))
        Text(errorMessage, textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineSmall)
        if (retry != null) {
            Spacer(Modifier.height(24.dp))
            OutlinedButton(retry) {
                Text(R.string.retry_action.str())
            }
        }
    }
}
