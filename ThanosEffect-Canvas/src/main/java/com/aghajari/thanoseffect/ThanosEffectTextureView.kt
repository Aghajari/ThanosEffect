package com.aghajari.thanoseffect

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import java.util.LinkedList
import kotlin.concurrent.Volatile

internal class ThanosEffectTextureView(
    context: Context,
) : TextureView(context),
    SurfaceTextureListener {

    private val thread: EffectRendererThread
    private val location = IntArray(2)
    private val viewRenderers = LinkedList<ViewRenderer>()

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isOpaque = false
    }

    private var sumOfPendingValues = 0
    private var destroyed = false
    private val checkDestroy = Runnable {
        if (destroyed.not() && isEmpty()) {
            destroy()
        }
    }

    init {
        thread = EffectRendererThread()
        surfaceTextureListener = this
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        getLocationInWindow(location)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (!destroyed && thread.running) {
            thread.start()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        thread.halt()
        clear()
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )
    }

    fun attach(
        view: View,
        pending: Int,
        drawRectParticles: Boolean,
        perPx: Int,
        particleLifeTime: ParticleLifeTime
    ) {
        if (destroyed) {
            return
        }

        synchronized(viewRenderers) {
            val renderer = ViewRenderer(
                view = view,
                surfaceLocation = location,
                sumOfPendingValues = pending + sumOfPendingValues,
                drawRectParticles = drawRectParticles,
                perPx = perPx,
                particleLifeTime = particleLifeTime,
            )
            viewRenderers.add(renderer)
            sumOfPendingValues += renderer.pendingValue
        }
    }

    fun pause(pause: Boolean) {
        thread.paused = pause
    }

    private fun clear() {
        synchronized(viewRenderers) {
            viewRenderers.clear()
        }
    }

    private fun isEmpty(): Boolean {
        synchronized(viewRenderers) {
            return viewRenderers.isEmpty()
        }
    }

    private fun destroy() {
        destroyed = true
        thread.halt()
        ThanosEffect.destroy()
    }

    private inner class EffectRendererThread : Thread() {
        @Volatile
        var running: Boolean = true

        @Volatile
        var paused: Boolean = false

        fun halt() {
            running = false
        }

        override fun run() {
            var lastTime = System.nanoTime()
            while (running) {
                val now = System.nanoTime()
                val deltaTime = EffectUtils.timeTuning(now, lastTime)
                lastTime = now

                while (paused) {
                    try {
                        sleep(1000)
                    } catch (ignore: Exception) {
                    }
                }

                synchronized(viewRenderers) {
                    val time = (deltaTime * 1000).toFloat()
                    for (viewParticle in viewRenderers) {
                        viewParticle.renderNextLinesIfReady()
                    }

                    lockCanvas(null)?.let { canvas ->
                        try {
                            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
                            drawParticles(canvas, time)
                        } finally {
                            unlockCanvasAndPost(canvas)
                        }
                    }
                    if (destroyed.not() && viewRenderers.isEmpty()) {
                        EffectUtils.runOnUIThread(checkDestroy, 30)
                    }
                }
            }
        }

        private fun drawParticles(canvas: Canvas, deltaTime: Float) {
            val each = viewRenderers.iterator()
            while (each.hasNext()) {
                val renderer = each.next()
                if (!renderer.draw(canvas, paint, deltaTime)) {
                    sumOfPendingValues -= renderer.pendingValue
                    each.remove()
                }
            }
        }
    }
}