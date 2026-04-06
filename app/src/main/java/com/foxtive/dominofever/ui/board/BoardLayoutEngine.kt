package com.foxtive.dominofever.ui.board

import com.foxtive.dominofever.model.BranchDirection
import com.foxtive.dominofever.model.TableState

class BoardLayoutEngine {

    fun build(table: TableState, width: Int, height: Int): BoardRenderState {
        if (table.placed.isEmpty()) {
            return BoardRenderState(emptyList(), emptyList())
        }

        val centerX = width / 2f
        val centerY = height / 2f
        val step = (minOf(width, height) / 7f).coerceAtLeast(70f)

        val positions = mutableMapOf<Int, Pair<Float, Float>>()
        val directions = mutableMapOf<Int, BranchDirection>()

        val first = table.placed.first()
        positions[first.id] = centerX to centerY
        directions[first.id] = BranchDirection.RIGHT

        val endpointRenders = mutableMapOf<Int, EndpointRender>()
        val remainingBranches = table.branches.toMutableList()

        repeat(64) {
            if (remainingBranches.isEmpty()) return@repeat
            var progress = false
            val iterator = remainingBranches.iterator()
            while (iterator.hasNext()) {
                val branch = iterator.next()
                val parentPos = positions[branch.parentDominoId] ?: continue
                var previous = parentPos

                branch.chain.forEach { dominoId ->
                    val existing = positions[dominoId]
                    if (existing != null) {
                        previous = existing
                    } else {
                        val next = offset(previous, branch.direction, step)
                        positions[dominoId] = next
                        directions[dominoId] = branch.direction
                        previous = next
                    }
                }

                val endpointPos = offset(previous, branch.direction, step)
                endpointRenders[branch.id] = EndpointRender(
                    branchId = branch.id,
                    value = branch.openValue,
                    x = endpointPos.first,
                    y = endpointPos.second,
                    direction = branch.direction
                )

                iterator.remove()
                progress = true
            }

            if (!progress) return@repeat
        }

        val tiles = table.placed.map { placed ->
            val pos = positions[placed.id] ?: (centerX to centerY)
            TileRender(
                dominoId = placed.id,
                domino = placed.domino,
                ownerId = placed.ownerId,
                x = pos.first,
                y = pos.second,
                direction = directions[placed.id] ?: BranchDirection.RIGHT
            )
        }

        return BoardRenderState(
            tiles = tiles,
            endpoints = endpointRenders.values.sortedBy { it.branchId }
        )
    }

    private fun offset(
        point: Pair<Float, Float>,
        direction: BranchDirection,
        step: Float
    ): Pair<Float, Float> {
        return (point.first + direction.dx * step) to (point.second + direction.dy * step)
    }
}
