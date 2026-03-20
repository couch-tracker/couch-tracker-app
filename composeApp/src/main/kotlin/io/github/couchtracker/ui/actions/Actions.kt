package io.github.couchtracker.ui.actions

data class Actions(
    val mainAction: Action? = null,
    val otherActions: List<Action> = emptyList(),
) {
    fun isNotEmpty(): Boolean {
        return mainAction != null || otherActions.isNotEmpty()
    }
}
