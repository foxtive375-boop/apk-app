package com.foxtive.dominofever.ui.board

import com.foxtive.dominofever.model.BranchDirection
import com.foxtive.dominofever.model.Domino

data class TileRender(
    val dominoId: Int,
    val domino: Domino,
    val ownerId: Int,
    val x: Float,
    val y: Float,
    val direction: BranchDirection
)

data class EndpointRender(
    val branchId: Int,
    val value: Int,
    val x: Float,
    val y: Float,
    val direction: BranchDirection
)

data class BoardRenderState(
    val tiles: List<TileRender>,
    val endpoints: List<EndpointRender>
)
