package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InfoFooter(text: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Icon(Icons.Outlined.Info, contentDescription = null, modifier = Modifier.padding(vertical = 8.dp))
        Text(text)
    }
}
