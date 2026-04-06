package com.foxtive.dominofever.ui.board

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.foxtive.dominofever.R
import com.foxtive.dominofever.model.BranchDirection
import com.foxtive.dominofever.model.TableState
import kotlin.math.min

class DominoBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val layoutEngine = BoardLayoutEngine()

    private var tableState: TableState = TableState()
    private var highlightedBranches: Set<Int> = emptySet()

    private var animatedDominoId: Int? = null
    private var animationScale: Float = 1f

    private val feltPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dominoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.domino_face)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = ContextCompat.getColor(context, R.color.domino_stroke)
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = ContextCompat.getColor(context, R.color.domino_stroke)
    }
    private val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.pip)
    }
    private val endpointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.endpoint)
    }
    private val endpointHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.endpoint_active)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 28f
        isFakeBoldText = true
    }

    fun setTableState(state: TableState, lastPlacedDominoId: Int?) {
        val previousCount = tableState.placed.size
        tableState = state
        if (state.placed.size > previousCount && lastPlacedDominoId != null) {
            animatedDominoId = lastPlacedDominoId
            startPlacementAnimation()
        }
        invalidate()
    }

    fun setHighlightedBranches(branchIds: Set<Int>) {
        highlightedBranches = branchIds
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        feltPaint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            ContextCompat.getColor(context, R.color.felt_dark),
            ContextCompat.getColor(context, R.color.felt_light),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), feltPaint)

        val render = layoutEngine.build(tableState, width, height)

        render.endpoints.forEach { endpoint ->
            val paint = if (endpoint.branchId in highlightedBranches) endpointHighlightPaint else endpointPaint
            canvas.drawCircle(endpoint.x, endpoint.y, 30f, paint)
            canvas.drawText(endpoint.value.toString(), endpoint.x, endpoint.y + 10f, textPaint)
        }

        render.tiles.forEach { tile ->
            val scale = if (tile.dominoId == animatedDominoId) animationScale else 1f
            drawDomino(canvas, tile, scale)
        }
    }

    private fun drawDomino(canvas: Canvas, tile: TileRender, scale: Float) {
        val baseWidth = min(180f, width / 4.5f)
        val baseHeight = baseWidth / 2f
        val rect = RectF(-baseWidth / 2f, -baseHeight / 2f, baseWidth / 2f, baseHeight / 2f)

        canvas.save()
        canvas.translate(tile.x, tile.y)
        canvas.scale(scale, scale)

        val rotation = when (tile.direction) {
            BranchDirection.LEFT,
            BranchDirection.RIGHT -> 0f
            BranchDirection.UP,
            BranchDirection.DOWN -> 90f
        }
        canvas.rotate(rotation)

        canvas.drawRoundRect(rect, 18f, 18f, dominoPaint)
        canvas.drawRoundRect(rect, 18f, 18f, strokePaint)
        canvas.drawLine(0f, rect.top, 0f, rect.bottom, dividerPaint)

        drawPips(canvas, tile.domino.left, centerX = -baseWidth / 4f, centerY = 0f, scale = baseWidth / 180f)
        drawPips(canvas, tile.domino.right, centerX = baseWidth / 4f, centerY = 0f, scale = baseWidth / 180f)

        canvas.restore()
    }

    private fun drawPips(canvas: Canvas, value: Int, centerX: Float, centerY: Float, scale: Float) {
        val offset = 20f * scale
        val pipRadius = 4.5f * scale

        fun pip(x: Float, y: Float) {
            canvas.drawCircle(centerX + x, centerY + y, pipRadius, pipPaint)
        }

        when (value) {
            1 -> pip(0f, 0f)
            2 -> {
                pip(-offset, -offset)
                pip(offset, offset)
            }
            3 -> {
                pip(-offset, -offset)
                pip(0f, 0f)
                pip(offset, offset)
            }
            4 -> {
                pip(-offset, -offset)
                pip(offset, -offset)
                pip(-offset, offset)
                pip(offset, offset)
            }
            5 -> {
                pip(-offset, -offset)
                pip(offset, -offset)
                pip(0f, 0f)
                pip(-offset, offset)
                pip(offset, offset)
            }
            6 -> {
                pip(-offset, -offset)
                pip(offset, -offset)
                pip(-offset, 0f)
                pip(offset, 0f)
                pip(-offset, offset)
                pip(offset, offset)
            }
        }
    }

    private fun startPlacementAnimation() {
        ValueAnimator.ofFloat(0.75f, 1f).apply {
            duration = 220
            addUpdateListener { animator ->
                animationScale = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }
}
