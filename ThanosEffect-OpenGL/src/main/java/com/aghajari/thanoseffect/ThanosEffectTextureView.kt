package com.aghajari.thanoseffect

import android.content.Context
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.GLES31
import android.opengl.Matrix
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import android.view.View
import android.view.ViewGroup
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.LinkedList
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import kotlin.concurrent.Volatile
import kotlin.random.Random
import com.aghajari.thanoseffect.EffectUtils.safeRun

internal class ThanosEffectTextureView(
    context: Context,
) : TextureView(context),
    SurfaceTextureListener {

    private var thread: EffectRendererThread? = null
    private val location = IntArray(2)
    private val viewRenderers = LinkedList<ViewRenderer>()

    private var sumOfPendingValues = 0
    private var destroyed = false
    private val checkDestroy = Runnable {
        if (destroyed.not() && isEmpty()) {
            destroy()
        }
    }

    init {
        surfaceTextureListener = this
        isOpaque = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        getLocationInWindow(location)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (thread == null) {
            thread = EffectRendererThread(surface, width, height)
            thread?.start()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        thread?.updateSize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        thread?.halt()
        thread = null
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
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
                inTime = thread?.elapsedTime ?: 0f,
                sumOfPendingValues = pending + sumOfPendingValues,
                perPx = perPx,
                particleLifeTime = particleLifeTime,
            )
            viewRenderers.add(renderer)
            sumOfPendingValues += renderer.pendingValue
        }
        if (thread == null || thread?.renderedFirstFrame == true) {
            (parent as? ViewGroup)?.postInvalidate()
        }
    }

    fun pause(pause: Boolean) {
        thread?.paused = pause
    }

    private fun isEmpty(): Boolean {
        synchronized(viewRenderers) {
            return viewRenderers.isEmpty()
        }
    }

    private fun destroy() {
        destroyed = true
        thread?.halt()
        thread = null
        ThanosEffect.destroy()

        synchronized(viewRenderers) {
            for (vp in viewRenderers) {
                vp.die()
            }
            viewRenderers.clear()
        }
    }

    fun renderFirstFrameByCanvasIfNeeded(canvas: Canvas) {
        if (thread == null || thread?.renderedFirstFrame == false) {
            synchronized(viewRenderers) {
                for (vp in viewRenderers) {
                    vp.draw(canvas)
                }
            }
        }
    }

    private inner class EffectRendererThread(
        private val surfaceTexture: SurfaceTexture,
        private var width: Int,
        private var height: Int
    ) :
        Thread() {
        @Volatile
        private var running = true

        @Volatile
        var paused = false

        @Volatile
        private var isClear = false

        private val resizeLock = Any()
        private var resize = false
        private var shouldCheckDestroy = false
        var renderedFirstFrame = false

        private val projectionMatrix = FloatArray(16)
        private var particles: FloatBuffer? = null
        private var drawOrder: ShortBuffer? = null

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
            init()
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
                val time = deltaTime * 1000f
                shouldCheckDestroy = false
                synchronized(viewRenderers) {
                    val each = viewRenderers.iterator()
                    while (each.hasNext()) {
                        val renderer = each.next()
                        if (!renderer.update(particles!!, time.toFloat())) {
                            shouldCheckDestroy = true
                            each.remove()
                        }
                    }

                    if (viewRenderers.isEmpty() && shouldCheckDestroy) {
                        EffectUtils.runOnUIThread(checkDestroy, 30)
                    }
                    drawFrame(time.toFloat())
                }

                if (!renderedFirstFrame) {
                    renderedFirstFrame = true
                    (parent as? ViewGroup)?.postInvalidate()
                }
            }
            die()
        }

        private var egl: EGL10? = null
        private var eglDisplay: EGLDisplay? = null
        private var eglConfig: EGLConfig? = null
        private var eglSurface: EGLSurface? = null
        private var eglContext: EGLContext? = null

        private var drawProgram = 0
        private var handlers: Handlers? = null

        private fun init() {
            egl = EGLContext.getEGL() as EGL10

            eglDisplay = egl!!.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay === EGL10.EGL_NO_DISPLAY) {
                running = false
                return
            }
            val version = IntArray(2)
            if (!egl!!.eglInitialize(eglDisplay, version)) {
                running = false
                return
            }

            val configAttributes = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
                EGL14.EGL_NONE
            )
            val eglConfigs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!egl!!.eglChooseConfig(eglDisplay, configAttributes, eglConfigs, 1, numConfigs)) {
                running = false
                return
            }
            eglConfig = eglConfigs[0]

            val contextAttributes = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL14.EGL_NONE
            )
            eglContext = egl!!.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL10.EGL_NO_CONTEXT,
                contextAttributes
            )
            if (eglContext == null) {
                running = false
                return
            }

            eglSurface = egl!!.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null)
            if (eglSurface == null) {
                running = false
                return
            }

            if (!egl!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                running = false
                return
            }

            // draw program (vertex and fragment shaders)
            val vertexShader = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER)
            val fragmentShader = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER)
            if (vertexShader == 0 || fragmentShader == 0) {
                running = false
                return
            }
            GLES31.glShaderSource(
                vertexShader,
                EffectUtils.readRes(context, R.raw.dust_vertex) + "\n// " + Math.random()
            )
            GLES31.glCompileShader(vertexShader)
            val status = IntArray(1)
            GLES31.glGetShaderiv(vertexShader, GLES31.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                GLES31.glDeleteShader(vertexShader)
                running = false
                return
            }
            GLES31.glShaderSource(
                fragmentShader,
                EffectUtils.readRes(context, R.raw.dust_fragment) + "\n// " + Math.random()
            )
            GLES31.glCompileShader(fragmentShader)
            GLES31.glGetShaderiv(fragmentShader, GLES31.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0) {
                GLES31.glDeleteShader(fragmentShader)
                running = false
                return
            }
            drawProgram = GLES31.glCreateProgram()
            if (drawProgram == 0) {
                running = false
                return
            }
            GLES31.glAttachShader(drawProgram, vertexShader)
            GLES31.glAttachShader(drawProgram, fragmentShader)

            GLES31.glLinkProgram(drawProgram)
            GLES31.glGetProgramiv(drawProgram, GLES31.GL_LINK_STATUS, status, 0)
            if (status[0] == 0) {
                running = false
                return
            }

            GLES31.glViewport(0, 0, width, height)

            GLES31.glEnable(GLES31.GL_BLEND)
            GLES31.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

            GLES31.glUseProgram(drawProgram)
            updateProjectionMatrix()
            GLES31.glUniform1f(
                GLES31.glGetUniformLocation(drawProgram, "seed"),
                Random.nextInt(256) / 256f
            )
            handlers = Handlers(drawProgram)

            particles =
                ByteBuffer.allocateDirect(EffectUtils.PARTICLE_BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()

            drawOrder =
                ByteBuffer.allocateDirect(EffectUtils.DRAW_ORDER.size * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
            drawOrder?.put(EffectUtils.DRAW_ORDER)
            drawOrder?.position(0)
        }

        private fun updateProjectionMatrix() {
            Matrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
            GLES31.glUniformMatrix4fv(
                GLES31.glGetUniformLocation(drawProgram, "projectionMatrix"),
                1,
                false,
                projectionMatrix,
                0
            )
        }

        var elapsedTime = 0f

        private fun drawFrame(time: Float) {
            if (destroyed || egl?.eglMakeCurrent(
                    eglDisplay,
                    eglSurface,
                    eglSurface,
                    eglContext
                ) == false
            ) {
                running = false
                return
            }
            elapsedTime += time

            val isEmpty: Boolean = viewRenderers.isEmpty()
            if (isEmpty && isClear) {
                return
            }

            GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)

            handlers?.let { h ->
                h.updateTime(elapsedTime)
                for (vp in viewRenderers) {
                    vp.draw(
                        handlers = h,
                        drawOrderBuffer = drawOrder,
                    )
                }
            }
            egl?.eglSwapBuffers(eglDisplay, eglSurface)
            isClear = isEmpty
        }

        private fun die() {
            particles = null
            checkDestroy.run()
            if (drawProgram != 0) {
                safeRun { GLES31.glDeleteProgram(drawProgram) }
                drawProgram = 0
            }
            if (egl != null) {
                safeRun {
                    egl?.eglMakeCurrent(
                        eglDisplay,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT
                    )
                }
                safeRun { egl?.eglDestroySurface(eglDisplay, eglSurface) }
                safeRun { egl?.eglDestroyContext(eglDisplay, eglContext) }
            }
            safeRun { surfaceTexture.release() }
        }

        private fun checkResize() {
            synchronized(resizeLock) {
                if (resize) {
                    if (!destroyed && eglContext != null) {
                        GLES31.glViewport(0, 0, width, height)
                        updateProjectionMatrix()
                    }
                    resize = false
                }
            }
        }
    }
}