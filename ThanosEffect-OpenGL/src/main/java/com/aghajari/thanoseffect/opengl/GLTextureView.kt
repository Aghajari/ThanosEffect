package com.aghajari.thanoseffect.opengl

import android.content.Context
import android.graphics.Canvas
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.GLES31
import android.opengl.Matrix
import android.view.ViewGroup
import com.aghajari.thanoseffect.opengl.GLUtils.safeRun
import com.aghajari.thanoseffect.core.RenderConfigs
import com.aghajari.thanoseffect.core.ThanosEffectTextureView
import com.aghajari.thanoseffect.widget.EffectedView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import kotlin.concurrent.Volatile
import kotlin.random.Random

/**
 * A custom TextureView for rendering Thanos effects using OpenGL.
 */
internal class GLTextureView(
    context: Context,
) : ThanosEffectTextureView<GLViewRenderer>(context) {

    @Volatile
    private var isClear = false

    private var renderedFirstFrame = false
    private val projectionMatrix = FloatArray(16)
    private var particles: FloatBuffer? = null
    private var drawOrder: ShortBuffer? = null

    private var elapsedTime = 0f

    private var egl: EGL10? = null
    private var eglDisplay: EGLDisplay? = null
    private var eglConfig: EGLConfig? = null
    private var eglSurface: EGLSurface? = null
    private var eglContext: EGLContext? = null

    private var drawProgram = 0
    private var handlers: Handlers? = null

    init {
        isOpaque = false
    }

    override fun onNewViewRendererAttached() {
        (parent as? ViewGroup)?.postInvalidate()
    }

    override fun createViewRenderer(
        view: EffectedView,
        surfaceLocation: IntArray,
        pendingWeight: Int,
        renderConfigs: RenderConfigs,
        onFirstFrameRenderedCallback: () -> Unit,
    ) = GLViewRenderer(
        view = view,
        surfaceLocation = surfaceLocation,
        inTime = elapsedTime,
        sumOfPendingWeights = pendingWeight,
        configs = renderConfigs,
        onFirstFrameRenderedCallback = onFirstFrameRenderedCallback,
    )

    override fun destroy() {
        synchronized(viewRenderers) {
            viewRenderers.forEach {
                it.die()
            }
            viewRenderers.clear()
        }
    }

    override fun initialize() {
        egl = EGLContext.getEGL() as EGL10

        eglDisplay = egl!!.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay === EGL10.EGL_NO_DISPLAY) {
            stopThread()
            return
        }
        val version = IntArray(2)
        if (!egl!!.eglInitialize(eglDisplay, version)) {
            stopThread()
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
            stopThread()
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
            stopThread()
            return
        }

        eglSurface = egl!!.eglCreateWindowSurface(eglDisplay, eglConfig, surfaceTexture, null)
        if (eglSurface == null) {
            stopThread()
            return
        }

        if (!egl!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            stopThread()
            return
        }

        // draw program (vertex and fragment shaders)
        val vertexShader = GLES31.glCreateShader(GLES31.GL_VERTEX_SHADER)
        val fragmentShader = GLES31.glCreateShader(GLES31.GL_FRAGMENT_SHADER)
        if (vertexShader == 0 || fragmentShader == 0) {
            stopThread()
            return
        }
        GLES31.glShaderSource(
            vertexShader,
            GLUtils.readRes(context, R.raw.dust_vertex) + "\n// " + Math.random()
        )
        GLES31.glCompileShader(vertexShader)
        val status = IntArray(1)
        GLES31.glGetShaderiv(vertexShader, GLES31.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            GLES31.glDeleteShader(vertexShader)
            stopThread()
            return
        }
        GLES31.glShaderSource(
            fragmentShader,
            GLUtils.readRes(context, R.raw.dust_fragment) + "\n// " + Math.random()
        )
        GLES31.glCompileShader(fragmentShader)
        GLES31.glGetShaderiv(fragmentShader, GLES31.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            GLES31.glDeleteShader(fragmentShader)
            stopThread()
            return
        }
        drawProgram = GLES31.glCreateProgram()
        if (drawProgram == 0) {
            stopThread()
            return
        }
        GLES31.glAttachShader(drawProgram, vertexShader)
        GLES31.glAttachShader(drawProgram, fragmentShader)

        GLES31.glLinkProgram(drawProgram)
        GLES31.glGetProgramiv(drawProgram, GLES31.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            stopThread()
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
            ByteBuffer.allocateDirect(GLUtils.PARTICLE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

        drawOrder =
            ByteBuffer.allocateDirect(GLUtils.DRAW_ORDER.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
        drawOrder?.put(GLUtils.DRAW_ORDER)
        drawOrder?.position(0)
    }

    internal fun renderFirstFrameByCanvasIfNeeded(canvas: Canvas) {
        if (renderedFirstFrame.not()) {
            synchronized(viewRenderers) {
                for (vp in viewRenderers) {
                    vp.draw(canvas)
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        if (eglContext != null) {
            GLES31.glViewport(0, 0, width, height)
            updateProjectionMatrix()
        }
    }

    override fun drawFrame(deltaTime: Float) {
        synchronized(viewRenderers) {
            val each = viewRenderers.iterator()
            while (each.hasNext()) {
                val renderer = each.next()
                if (!renderer.update(particles!!, deltaTime)) {
                    each.remove()
                }
            }

            drawGLFrame(deltaTime)
        }

        if (!renderedFirstFrame) {
            renderedFirstFrame = true
            (parent as? ViewGroup)?.postInvalidate()
        }
    }

    override fun die() {
        particles = null
        forceDestroy()
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
    }

    private fun drawGLFrame(time: Float) {
        if (destroyed || egl?.eglMakeCurrent(
                eglDisplay,
                eglSurface,
                eglSurface,
                eglContext
            ) == false
        ) {
            stopThread()
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
}