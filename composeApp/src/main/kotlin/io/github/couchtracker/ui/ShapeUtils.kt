package io.github.couchtracker.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ListItemShape(
    isFirstInList: Boolean,
    isLastInList: Boolean,
    smallShape: CornerBasedShape = MaterialTheme.shapes.extraSmall,
    largeShape: CornerBasedShape = MaterialTheme.shapes.large,
): CornerBasedShape {
    return smallShape.copy(
        topStart = if (isFirstInList) largeShape.topStart else smallShape.topStart,
        topEnd = if (isFirstInList) largeShape.topEnd else smallShape.topEnd,
        bottomStart = if (isLastInList) largeShape.bottomStart else smallShape.bottomStart,
        bottomEnd = if (isLastInList) largeShape.bottomEnd else smallShape.bottomEnd,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListItemShapes(
    isFirstInList: Boolean,
    isLastInList: Boolean,
    smallShape: CornerBasedShape = MaterialTheme.shapes.extraSmall,
    largeShape: CornerBasedShape = MaterialTheme.shapes.large,
): ListItemShapes {
    return ListItemDefaults.shapes(
        shape = ListItemShape(
            isFirstInList = isFirstInList,
            isLastInList = isLastInList,
            smallShape = smallShape,
            largeShape = largeShape,
        ),
    )
}
