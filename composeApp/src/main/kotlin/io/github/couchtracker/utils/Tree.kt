package io.github.couchtracker.utils

/**
 * A [MixedValueTree] that holds the same value ([T]) in all nodes.
 */
typealias Tree<T> = MixedValueTree<T, T, T>

/**
 * Tree that holds different types of values for each type of node (root, intermediate, leaf).
 *
 * @param R the type of value held by a [Root] node.
 * @param I the type of value held by an [Intermediate] (internal, non-root) node.
 * @param V the type of value held by a [Leaf] node.
 */
sealed interface MixedValueTree<out R, out I, out V> {

    /**
     * Any node that is not the root node
     */
    sealed interface NonRoot<out I, out V> : MixedValueTree<Nothing, I, V>

    /**
     * Any node that is not a leaf (i.e. has children)
     */
    sealed interface Internal<out I, out V> : MixedValueTree<Nothing, I, V> {
        val children: List<NonRoot<I, V>>
    }

    /**
     * The root of a tree, holding a value of type [R].
     */
    data class Root<out R, out I, out V>(
        val value: R,
        override val children: List<NonRoot<I, V>>,
    ) : Internal<I, V>

    /**
     * An internal node that is not the [Root], holding a value of type [I].
     */
    data class Intermediate<out I, out V>(
        val value: I,
        override val children: List<NonRoot<I, V>>,
    ) : Internal<I, V>, NonRoot<I, V>

    /**
     * A leaf node, holding a value of type [V].
     */
    data class Leaf<out V>(val value: V) : NonRoot<Nothing, V>
}

/**
 * Recursively returns all the leafs in this subtree.
 */
fun <V> MixedValueTree.Internal<*, V>.allLeafs(): Sequence<MixedValueTree.Leaf<V>> {
    return sequence {
        for (child in children) {
            when (child) {
                is MixedValueTree.Leaf -> yield(child)
                is MixedValueTree.Intermediate -> yieldAll(child.allLeafs())
            }
        }
    }
}

fun MixedValueTree.Internal<*, *>.countLeafs(): Int {
    return children.sumOf {
        when (it) {
            is MixedValueTree.Leaf<*> -> 1
            is MixedValueTree.Intermediate<*, *> -> it.countLeafs()
        }
    }
}

fun <V> MixedValueTree.Internal<*, V>.findLeafValue(predicate: (V) -> Boolean): V? {
    return allLeafs().find { predicate(it.value) }?.value
}
