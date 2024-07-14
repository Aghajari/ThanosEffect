package com.aghajari.thanoseffect.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import com.aghajari.thanoseffect.core.ThanosEffectTextureView
import com.aghajari.thanoseffect.core.RenderConfigs
import com.aghajari.thanoseffect.widget.EffectedView

/**
 * A custom TextureView for rendering Thanos effects using Android Canvas API.
 */
internal class CanvasTextureView(
    context: Context,
) : ThanosEffectTextureView<CanvasViewRenderer>(context) {

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isOpaque = false
    }

    override fun createViewRenderer(
        view: EffectedView,
        surfaceLocation: IntArray,
        pendingWeight: Int,
        renderConfigs: RenderConfigs
    ) = CanvasViewRenderer(
        view = view,
        surfaceLocation = surfaceLocation,
        sumOfPendingWeights = pendingWeight,
        configs = renderConfigs,
    )

    override fun destroy() {
    }

    override fun initialize() {
    }

    override fun drawFrame(deltaTime: Float) {
        synchronized(viewRenderers) {
            for (viewParticle in viewRenderers) {
                viewParticle.renderNextLinesIfReady()
            }

            lockCanvas(null)?.let { canvas ->
                try {
                    canvas.drawColor(0, PorterDuff.Mode.CLEAR)
                    drawParticles(canvas, deltaTime)
                } finally {
                    unlockCanvasAndPost(canvas)
                }
            }
        }
    }

    private fun drawParticles(canvas: Canvas, deltaTime: Float) {
        val each = viewRenderers.iterator()
        while (each.hasNext()) {
            val renderer = each.next()
            if (!renderer.draw(canvas, paint, deltaTime)) {
                removeRendererWeight(renderer)
                each.remove()
            }
        }
    }

    override fun die() {
    }
}