package com.android.launcher3.model

import androidx.compose.runtime.Immutable

/**
 * Immutable wrapper around standard List to guarantee Compose compiler stability
 * and prevent unnecessary recompositions of parent screens and grid components.
 */
@Immutable
data class ImmutableList<out T>(val items: List<T> = emptyList()) : List<T> by items {
    companion object {
        private val EMPTY = ImmutableList<Nothing>(emptyList())

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): ImmutableList<T> = EMPTY as ImmutableList<T>

        fun <T> of(items: List<T>): ImmutableList<T> = ImmutableList(items)
    }

    override fun toString(): String = items.toString()
}

fun <T> List<T>.toImmutableList(): ImmutableList<T> = ImmutableList(this)
