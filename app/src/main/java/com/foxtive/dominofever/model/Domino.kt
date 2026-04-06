package com.foxtive.dominofever.model

import kotlin.math.max
import kotlin.math.min

data class Domino(val left: Int, val right: Int) {
    init {
        require(left in 0..6) { "left side must be in range 0..6" }
        require(right in 0..6) { "right side must be in range 0..6" }
    }

    val isDouble: Boolean
        get() = left == right

    val pipSum: Int
        get() = left + right

    fun matches(value: Int): Boolean = left == value || right == value

    fun other(value: Int): Int {
        require(matches(value)) { "Domino $this does not match $value" }
        return when {
            left == value && right == value -> value
            left == value -> right
            else -> left
        }
    }

    fun normalizedKey(): String = "${min(left, right)}-${max(left, right)}"

    override fun toString(): String = "$left|$right"
}
