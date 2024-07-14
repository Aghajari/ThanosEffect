package com.aghajari.thanoseffect.core

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.TextureView
import com.aghajari.thanoseffect.ThanosEffect
import com.aghajari.thanoseffect.widget.EffectedView
import java.util.LinkedList

/**
 * Abstract class representing a TextureView for rendering Thanos effects.
 */
abstract class ThanosEffectTextureView<VR : ViewRenderer>(
    context: Context,
) : TextureView(context) {

    private var thread: EffectRendererThread? = null
    private val location = IntArray(2)
    protected val viewRenderers = LinkedList<VR>()

    protected var destroyed = false
        private set

    private var sumOfWeights = 0
    private val checkDestroy = Runnable {
        if (destroyed.not() && isEmpty()) {
            destroyed = true
            thread?.halt()
            thread = null
            destroy()
            ThanosEffect.destroyTextureView()
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

    /**
     * Attaches a new view renderer.
     *
     * @param view The view to attach.
     * @param pendingWeight The pending weight after the view.
     * @param renderConfigs The configuration parameters for rendering.
     */
    fun attach(
        view: EffectedView,
        pendingWeight: Int,
        renderConfigs: RenderConfigs,
    ) {
        if (destroyed) {
            return
        }

        synchronized(viewRenderers) {
            val renderer = createViewRenderer(
                view = view,
                surfaceLocation = location,
                pendingWeight = pendingWeight + sumOfWeights,
                renderConfigs = renderConfigs,
            )
            viewRenderers.add(renderer)
            sumOfWeights += renderer.weight
        }
        onNewViewRendererAttached()
    }

    /**
     * Pauses or resumes the rendering thread.
     *
     * @param pause `true` to pause, `false` to resume.
     */
    fun pause(pause: Boolean) {
        thread?.paused = pause
    }

    /**
     * Stops the rendering thread.
     */
    protected fun stopThread() {
        thread?.halt()
    }

    /**
     * Forces the destruction of the renderer.
     */
    fun forceDestroy() {
        clear()
        checkDestroy.run()
    }

    /**
     * Clears all view renderers.
     */
    private fun clear() {
        synchronized(viewRenderers) {
            viewRenderers.clear()
        }
    }

    /**
     * Checks if there are no view renderers.
     *
     * @return `true` if there are no view renderers, `false` otherwise.
     */
    private fun isEmpty(): Boolean {
        synchronized(viewRenderers) {
            return viewRenderers.isEmpty()
        }
    }

    /**
     * Removes the weight of a renderer.
     *
     * @param renderer The renderer to remove weight from.
     */
    protected fun removeRendererWeight(renderer: ViewRenderer) {
        sumOfWeights -= renderer.weight
    }

    /**
     * Creates a view renderer.
     *
     * @param view The view to render.
     * @param surfaceLocation The location of the surface.
     * @param pendingWeight The pending weight of the surface.
     * @param renderConfigs The configuration parameters for rendering.
     * @return The created view renderer.
     */
    protected abstract fun createViewRenderer(
        view: EffectedView,
        surfaceLocation: IntArray,
        pendingWeight: Int,
        renderConfigs: RenderConfigs
    ): VR

    /**
     * Destroys the renderer.
     */
    protected abstract fun destroy()

    /**
     * Initializes the renderer. Calls once the renderer thread started.
     */
    protected abstract fun initialize()

    /**
     * Draws a frame.
     *
     * @param deltaTime The time elapsed since the last frame.
     */
    protected abstract fun drawFrame(deltaTime: Float)

    /**
     * Cleans up resources when the thread dies.
     */
    protected abstract fun die()

    /**
     * Resizes the renderer.
     *
     * @param width The new width.
     * @param height The new height.
     */
    protected open fun resize(width: Int, height: Int) {}

    /**
     * Called when a new view renderer is attached.
     */
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