package com.aghajari.thanoseffect

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.view.View
import java.lang.ref.WeakReference

internal class ViewRenderer(
    view: View,
    surfaceLocation: IntArray,
    sumOfPendingValues: Int = 0,
    drawRectParticles: Boolean = false,
    perPx: Int = EffectUtils.calculateSize(view),
    private val particleLifeTime: ParticleLifeTime = ParticleLifeTimeImpl
) {

    private var view: WeakReference<View>? = WeakReference(view)
    private val particles: Array<Particle?>

    private val offset = Point()
    private val center = Point()
    private val bitmap: Bitmap

    private val src: Rect
    private val dst: Rect
    private val particleRect = if (drawRectParticles) {
        RectF()
    } else null

    private val maxLine: Int
    private var line = 0
    private var animatedLineWidth = 0
    private var currentParticleIndex = 0

    private val optimizedPerPx: Int
    val pendingValue: Int

    private var time = 0f
    private var nextRenderTime: Long = 0
    private var endRenderingTime = 0f

    init {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        offset.x = location[0] - surfaceLocation[0] - view.translationX.toInt()
        offset.y = location[1] - surfaceLocation[1] - view.translationY.toInt()

        bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))

        pendingValue = EffectUtils.calculatePendingSize(view)
        optimizedPerPx = EffectUtils.optimizeSize(perPx, sumOfPendingValues)

        src = Rect(0, 0, bitmap.width, bitmap.height)
        dst = Rect(
            offset.x,
            offset.y,
            bitmap.width + offset.x,
            bitmap.height + offset.y
        )

        particles = arrayOfNulls(bitmap.width * bitmap.height / (optimizedPerPx * optimizedPerPx))
        maxLine = bitmap.width / optimizedPerPx
        center.set(view.width / 2, view.height / 2)
        renderNextLinesIfReady()
    }

    fun draw(canvas: Canvas, paint: Paint, deltaTime: Float): Boolean {
        time += deltaTime

        val v: View? = view?.get()
        val saved = v != null && v.translationX != 0f && time <= 120f
        var save = 0

        if (saved) {
            // We need this for default translation animation
            // When items are selected in recyclerView
            save = canvas.save()
            canvas.translate(requireNotNull(v).translationX, 0f)
        } else if (view != null) {
            view?.clear()
            view = null
        }

        if (animatedLineWidth < bitmap.width) {
            canvas.drawBitmap(bitmap, src, dst, null)
        }

        val renderingLine = line.toFloat() / maxLine
        for (particle in particles) {
            if (particle == null) {
                break
            }
            if (!particle.update(deltaTime, offset, center, renderingLine)) {
                continue
            }
            paint.color = particle.color
            paint.alpha = particle.alpha

            if (particleRect != null) {
                particleRect.set(
                    particle.x - particle.r,
                    particle.y - particle.r,
                    particle.x + particle.r,
                    particle.y + particle.r,
                )
                canvas.drawRect(particleRect, paint)
            } else {
                canvas.drawCircle(particle.x, particle.y, particle.r, paint)
            }
        }

        if (saved) {
            canvas.restoreToCount(save)
        }
        return if (endRenderingTime == 0f) {
            true
        } else {
            time <= endRenderingTime + particleLifeTime.maxDuration
        }
    }

    fun renderNextLinesIfReady() {
        if (line >= maxLine) {
            if (endRenderingTime == 0f) {
                endRenderingTime = time
            }
            animatedLineWidth = bitmap.width
            return
        }

        var now = System.currentTimeMillis()
        val delay = EffectUtils.EACH_LINE_DURATION
        if (now < nextRenderTime) {
            return
        }
        var renderCount = 1
        if (nextRenderTime != 0L) {
            while (now - delay > nextRenderTime) {
                renderCount++
                now -= delay
            }
        }

        var c = 0
        while (c < renderCount && line < maxLine) {
            val x = line * optimizedPerPx
            animatedLineWidth = x
            src.left = animatedLineWidth
            dst.left = dst.right - bitmap.width + src.left

            var onThisLine = 0
            var y = 0
            while (y < bitmap.height) {
                if (EffectUtils.canDrawPixel(bitmap, x, y).not()) {
                    y += optimizedPerPx
                    continue
                }
                onThisLine++
                particles[currentParticleIndex++] = Particle(
                    initialX = x,
                    initialY = y,
                    initialR = EffectUtils.randomRadius(optimizedPerPx),
                    color = bitmap.getPixel(x, y),
                    lifeTime = particleLifeTime.calculateLifeTime(line, maxLine)
                )

                if (particles.size <= currentParticleIndex) {
                    animatedLineWidth = bitmap.width
                    line = maxLine
                    endRenderingTime = time
                    return
                }
                y += optimizedPerPx
            }
            line++

            if (onThisLine != 0) {
                c++
            }
        }
        nextRenderTime = now + delay
    }
}