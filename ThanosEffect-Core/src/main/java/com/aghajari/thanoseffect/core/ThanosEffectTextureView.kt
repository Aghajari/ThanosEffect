package com.aghajari.thanoseffect.core

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.view.View
import com.aghajari.thanoseffect.ThanosEffect
import com.aghajari.thanoseffect.widget.EffectedView
import java.util.LinkedList

abstract class ThanosEffectTextureView<VR : ViewRenderer>(
    context: Context,
) : TextureView(context) {

    private var thread: EffectRendererThread? = null
    private val location = IntArray(2)
    protected val viewRenderers = LinkedList<VR>()

    protected var destroyed = false
        private set

    private var sumOfPendingValues = 0
    private val checkDestroy = Runnable {
        if (destroyed.not() && isEmpty()) {
            destroyed = true
            thread?.halt()
            thread = null
            destroy()
            ThanosEffect.destroy()
        }
    }

    init {
        surfaceTextureListener = object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                if (thread == null) {
                    thread = EffectRendererThread(surface, width, height)
                    thread?.start()
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                thread?.updateSize(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                thread?.halt()
                thread = null
                clear()
                return true
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        getLocationInWindow(location)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )
    }

    fun attach(
        view: EffectedView,
        pending: Int,
        renderConfigs: RenderConfigs,
    ) {
        if (destroyed) {
            return
        }

        synchronized(viewRenderers) {
            val renderer = createViewRenderer(
                view = view,
                surfaceLocation = location,
                pending = pending + sumOfPendingValues,
                renderConfigs = renderConfigs,
            )
            viewRenderers.add(renderer)
            sumOfPendingValues += renderer.pendingValue
        }
        onNewViewRendererAttached()
    }

    fun pause(pause: Boolean) {
        thread?.paused = pause
    }

    protected fun stopThread() {
        thread?.halt()
    }

    protected fun forceDestroy() {
        checkDestroy.run()
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

    protected fun removeRendererWeight(renderer: ViewRenderer) {
        sumOfPendingValues -= renderer.pendingValue
    }

    protected abstract fun createViewRenderer(
        view: EffectedView,
        surfaceLocation: IntArray,
        pending: Int,
        renderConfigs: RenderConfigs
    ): VR

    protected abstract fun destroy()

    protected abstract fun initialize()
    protected abstract fun drawFrame(deltaTime: Float)
    protected abstract fun die()

    protected open fun resize(width: Int, height: Int) {}
    protected open fun onNewViewRendererAttached() {}

    private inner class EffectRendererThread(
        private val surfaceTexture: SurfaceTexture,
        private var width: Int,
        private var height: Int,
    ) : Thread() {
        @Volatile
        var running: Boolean = true

        @Volatile
        var paused: Boolean = false

        private val resizeLock = Any()
        private var resize = false

        fun updateSize(width: Int, height: Int) {
            synchronized(resizeLock) {
                resize = true
                this.width = width
                this.height = height
            }
        }

        fun halt() {
            running = false
        }

        override fun run() {
            initialize()
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
                checkResize()

                drawFrame((deltaTime * 1000).toFloat())

                if (destroyed.not() && isEmpty()) {
                    EffectUtils.runOnUIThread(checkDestroy, 30)
                }
            }
            die()
            try {
                surfaceTexture.release()
            } catch (ignore: Exception) {
            }
        }

        private fun checkResize() {
            synchronized(resizeLock) {
                if (resize) {
                    if (destroyed.not()) {
                        resize(width, height)
                    }
                    resize = false
                }
            }
        }
    }
}