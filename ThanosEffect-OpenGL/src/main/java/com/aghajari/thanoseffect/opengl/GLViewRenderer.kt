package com.aghajari.thanoseffect.opengl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.opengl.GLES31
import android.opengl.GLUtils
import com.aghajari.thanoseffect.opengl.GLUtils.PARTICLE_BYTES
import com.aghajari.thanoseffect.core.EffectUtils
import com.aghajari.thanoseffect.opengl.GLUtils.safeRun
import com.aghajari.thanoseffect.core.RenderConfigs
import com.aghajari.thanoseffect.core.ViewRenderer
import com.aghajari.thanoseffect.widget.EffectedView
import java.lang.ref.WeakReference
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.max
import kotlin.math.min

internal class GLViewRenderer(
    view: EffectedView,
    surfaceLocation: IntArray,
    private val inTime: Float,
    sumOfPendingValues: Int = 0,
    private val configs: RenderConfigs,
) : ViewRenderer {

    private val location = IntArray(2)
    private var view: WeakReference<EffectedView>? = WeakReference(view)
    private val particlesSize: Int
    private var bitmap: Bitmap? = null
    private val bounds: Rect
    private val src: FloatArray
    private val dst: FloatArray
    private val srcNorm: FloatArray

    private val maxLine: Int
    private val bitmapWidth: Int
    private val bitmapHeight: Int
    private var line = 0
    private var animatedLineWidth = 0
    private var currentParticleIndex = 0
    private var readySize = 0

    private val optimizedPerPx: Int
    override val pendingValue: Int

    private var time = 0f
    private var nextRenderTime: Long = 0
    private var endRenderingTime = 0f
    private var maxLifetime = 0f
    private var renderedLastFrame = false

    private var initialized = false
    private var buffer: IntArray? = IntArray(2)

    init {
        view.getLocationInWindow(location)
        location[0] = location[0] - surfaceLocation[0] - view.translationX.toInt()
        location[1] = location[1] - surfaceLocation[1] - view.translationY.toInt()

        val bitmap = view.createBitmap()
        this.bitmap = bitmap
        bitmapWidth = bitmap.width
        bitmapHeight = bitmap.height

        pendingValue = EffectUtils.calculatePendingSize(view)
        optimizedPerPx = EffectUtils.optimizeSize(configs.perPx, sumOfPendingValues)

        src = floatArrayOf(0f, 0f, bitmapWidth.toFloat(), bitmapHeight.toFloat())
        srcNorm = floatArrayOf(0f, 0f, 1f, 1f)
        dst = floatArrayOf(
            location[0].toFloat(),
            location[1].toFloat(),
            (bitmapWidth + location[0]).toFloat(),
            (bitmapHeight + location[1]).toFloat()
        )

        particlesSize = bitmapWidth * bitmapHeight / (optimizedPerPx * optimizedPerPx)
        bounds = Rect(0, 0, bitmapWidth, bitmapHeight)
        maxLine = bitmapWidth / optimizedPerPx
    }


    fun update(particles: FloatBuffer, deltaTime: Float): Boolean {
        if (buffer == null) {
            return false
        }

        try {
            if (!initialized) {
                GLES31.glGenBuffers(1, buffer, 0)
                GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, requireNotNull(buffer)[0])
                GLES31.glBufferData(
                    GLES31.GL_ARRAY_BUFFER,
                    particlesSize * PARTICLE_BYTES,
                    null,
                    GLES31.GL_DYNAMIC_DRAW
                )

                GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
                GLES31.glGenTextures(1, buffer, 1)
                GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, requireNotNull(buffer)[1])
                GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
                GLES31.glGenerateMipmap(GLES31.GL_TEXTURE_2D)

                initialized = true
            }
        } catch (ignore: Throwable) {
            die()
            return false
        }
        renderNextLinesIfReady(particles)
        time += deltaTime

        return isRunning()
    }

    // Draws only the first frame of the graphics thread
    fun draw(canvas: Canvas) {
        if (bitmap == null) {
            return
        }

        var v: EffectedView? = null
        if (view != null) {
            v = view!!.get()
        }
        val saved = v != null && v.translationX != 0f && time <= 120f
        var save = 0

        if (saved) {
            save = canvas.save()
            canvas.translate(v!!.translationX, 0f)
        } else if (view != null) {
            view!!.clear()
            view = null
        }

        if (bitmap != null) {
            canvas.drawBitmap(
                bitmap!!,
                Rect(src[0].toInt(), src[1].toInt(), src[2].toInt(), src[3].toInt()),
                Rect(dst[0].toInt(), dst[1].toInt(), dst[2].toInt(), dst[3].toInt()),
                null
            )
        }

        if (saved) {
            canvas.restoreToCount(save)
        }
    }

    private fun isRunning(): Boolean {
        if (endRenderingTime == 0f) {
            return true
        } else {
            val isRunning = time <= maxLifetime + configs.lineDelay
            if (!isRunning) {
                die()
            }
            return isRunning
        }
    }

    private fun renderNextLinesIfReady(particles: FloatBuffer) {
        val now = System.currentTimeMillis()
        if (now < nextRenderTime) {
            return
        }
        val delay = configs.lineDelay
        val count: Int = if (bounds.width() > EffectUtils.dp(300f)) {
            EffectUtils.dp(2f)
        } else {
            EffectUtils.dp(1f)
        }

        if (line >= maxLine) {
            if (endRenderingTime == 0f) {
                endRenderingTime = time
            }
            animatedLineWidth = bitmapWidth
            if (!renderedLastFrame) {
                updateRect()
                renderedLastFrame = true
            }
            return
        }
        if (bitmap == null) {
            endRendering()
            return
        }

        var c = 0
        while (c < count) {
            val x = line * optimizedPerPx
            animatedLineWidth = x
            if (x < 0) {
                line++
                c++
                continue
            }
            if (x >= bitmapWidth || line >= maxLine) {
                endRendering()
                return
            }

            var onThisLine = 0
            var y = bounds.top
            while (y < bounds.bottom) {
                if (EffectUtils.canDrawPixel(bitmap!!, x, y).not()) {
                    y += optimizedPerPx
                    continue
                }
                onThisLine++
                particles.position(0)
                val lifeTime = configs.particleLifeTime.calculateLifeTime(line, maxLine).toFloat()
                val color = bitmap!!.getPixel(x, y)
                val vy = EffectUtils.randomVelocity().toFloat()
                val dx = EffectUtils.randomTranslation(initialMinDp = 32).toFloat()
                val dy = EffectUtils.randomTranslation(initialMinDp = 96).toFloat()
                val r = EffectUtils.randomRadius(optimizedPerPx)

                maxLifetime = max(maxLifetime, (lifeTime + time))
                particles.put(x.toFloat())
                particles.put(y.toFloat())
                particles.put(Color.red(color) / 255f)
                particles.put(Color.green(color) / 255f)
                particles.put(Color.blue(color) / 255f)
                particles.put(EffectUtils.randomInitialAlpha(color) / 255f)
                particles.put(lifeTime)
                particles.put(inTime + time)
                particles.put(r)
                particles.put(vy)
                particles.put(dx)
                particles.put(dy)
                particles.position(0)
                GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, buffer!![0])
                GLES31.glBufferSubData(
                    GLES31.GL_ARRAY_BUFFER,
                    currentParticleIndex * PARTICLE_BYTES,
                    PARTICLE_BYTES,
                    particles
                )
                currentParticleIndex++

                if (particlesSize <= currentParticleIndex) {
                    endRendering()
                    return
                }
                y += optimizedPerPx
            }
            line++
            if (onThisLine > 0) {
                c++
            }
        }
        if (line >= maxLine) {
            endRendering()
        } else {
            readySize = currentParticleIndex
            nextRenderTime = now + delay
            updateRect()
        }
    }

    private fun updateRect() {
        src[0] = animatedLineWidth.toFloat()
        dst[0] = dst[2] - bitmapWidth + src[0]
        normalizeSrc()
    }

    private fun endRendering() {
        readySize = currentParticleIndex
        line = maxLine
        endRenderingTime = time
        updateRect()
    }

    private fun normalizeSrc() {
        srcNorm[0] = max(0.0f, src[0] / bitmapWidth)
        srcNorm[1] = src[1] / bitmapHeight
        srcNorm[2] = src[2] / bitmapWidth
        srcNorm[3] = src[3] / bitmapHeight
    }

    private fun destroyBitmap() {
        if (bitmap != null) {
            view = null
            bitmap?.recycle()
            bitmap = null
        }
    }

    fun draw(
        handlers: Handlers,
        drawOrderBuffer: ShortBuffer?
    ) {
        if (buffer == null) {
            return
        }

        var v: EffectedView? = null
        if (view != null) {
            v = view!!.get()
        }
        val tx = if (v != null && time <= 120f) v.translationX else 0f
        val ty = if (v != null && time <= 120f) v.translationY else 0f

        if (bitmap != null) {
            handlers.drawTexture(buffer!![1], tx, ty, srcNorm, dst, drawOrderBuffer)
        }
        handlers.draw(
            buffer = buffer!![0],
            tx = tx + location[0],
            ty = ty + location[1],
            optimizedPerPx = optimizedPerPx.toFloat(),
            centerX = bounds.centerX().toFloat(),
            centerY = bounds.centerY().toFloat(),
            rendering = min(1.0f, (line * 1f / maxLine)),
            drawCircle = configs.drawRectParticles.not(),
            particleCount = readySize,
        )
        if (renderedLastFrame && bitmap != null) {
            destroyBitmap()
        }
    }

    fun die() {
        destroyBitmap()
        if (buffer != null) {
            safeRun { GLES31.glDeleteBuffers(1, buffer, 0) }
            safeRun { GLES31.glDeleteTextures(1, buffer, 1) }
            buffer = null
        }
    }
}