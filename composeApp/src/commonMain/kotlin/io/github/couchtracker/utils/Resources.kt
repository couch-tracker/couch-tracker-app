package io.github.couchtracker.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun StringResource.str() = stringResource(this)

@Composable
fun StringResource.str(vararg formatArgs: Any) = stringResource(this, *formatArgs)
