package com.foxtive.dominofever.model

enum class BranchDirection(val dx: Int, val dy: Int) {
    LEFT(-1, 0),
    RIGHT(1, 0),
    UP(0, -1),
    DOWN(0, 1);

    fun opposite(): BranchDirection = when (this) {
        LEFT -> RIGHT
        RIGHT -> LEFT
        UP -> DOWN
        DOWN -> UP
    }

    fun turnLeft(): BranchDirection = when (this) {
        LEFT -> DOWN
        RIGHT -> UP
        UP -> LEFT
        DOWN -> RIGHT
    }

    fun turnRight(): BranchDirection = when (this) {
        LEFT -> UP
        RIGHT -> DOWN
        UP -> RIGHT
        DOWN -> LEFT
    }
}
