package io.github.couchtracker.ui

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ListItemShape(
    position: ItemPosition,
    smallShape: CornerBasedShape = MaterialTheme.shapes.extraSmall,
    largeShape: CornerBasedShape = MaterialTheme.shapes.large,
): CornerBasedShape {
    return smallShape.copy(
        topStart = if (position.isInTopStartCorner()) largeShape.topStart else smallShape.topStart,
        topEnd = if (position.isInTopEndCorner()) largeShape.topEnd else smallShape.topEnd,
        bottomStart = if (position.isInBottomStartCorner()) largeShape.bottomStart else smallShape.bottomStart,
        bottomEnd = if (position.isInBottomEndCorner()) largeShape.bottomEnd else smallShape.bottomEnd,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ListItemShapes(
    position: ItemPosition,
    smallShape: CornerBasedShape = MaterialTheme.shapes.extraSmall,
    largeShape: CornerBasedShape = MaterialTheme.shapes.large,
): ListItemShapes {
    return ListItemDefaults.shapes(
        shape = ListItemShape(
            position = position,
            smallShape = smallShape,
            largeShape = largeShape,
        ),
    )
}
