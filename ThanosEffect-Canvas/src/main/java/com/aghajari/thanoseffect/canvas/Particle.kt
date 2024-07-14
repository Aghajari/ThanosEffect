package com.aghajari.thanoseffect.canvas

import android.graphics.Point
import com.aghajari.thanoseffect.core.EffectUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Represents a particle in the Thanos effect animation.
 *
 * @property initialX Initial X coordinate of the particle.
 * @property initialY Initial Y coordinate of the particle.
 * @property initialR Initial radius of the particle.
 * @property color Color of the particle.
 * @property lifeTime Duration of the particle's life in milliseconds.
 * @property initialAlpha Initial alpha (transparency) of the particle.
 * @property velocityY Vertical velocity of the particle.
 * @property translationY Maximum vertical translation distance of the particle.
 * @property translationX Maximum horizontal translation distance of the particle.
 */
internal data class Particle(
    val initialX: Int,
    val initialY: Int,
    val initialR: Float,
    val color: Int,
    val lifeTime: Int,
    val initialAlpha: Int = EffectUtils.randomInitialAlpha(color),
    val velocityY: Int = EffectUtils.randomVelocity(),
    val translationY: Int = EffectUtils.randomTranslation(initialMinDp = 32),
    val translationX: Int = EffectUtils.randomTranslation(initialMinDp = 96),
) {

    private var time = 0f
    var alpha = initialAlpha
        private set
    var x = initialX.toFloat()
        private set
    var y = initialY.toFloat()
        private set
    var r = initialR
        private set

    /**
     * Updates the particle's state based on the elapsed time.
     *
     * @param deltaTime Time elapsed since the last update in milliseconds.
     * @param offset Offset point to be applied to the particle's position.
     * @param center Center point used for calculating translation.
     * @param renderingLine Factor representing the progress of the animation line.
     * @return `true` if the particle is still active, `false` if its lifetime has ended.
     */
    fun update(
        deltaTime: Float,
        offset: Point,
        center: Point,
        renderingLine: Float,
    ): Boolean {
        time += deltaTime
        val fraction = time / lifeTime
        if (fraction >= 1f) {
            return false
        }
        val renderingEffect = sqrt(min(1f, renderingLine))
        y = offset.y + initialY + (initialY - center.y * 1f) / center.y *
                translationY * fraction * renderingEffect
        y -= (fraction * velocityY).pow(2.0f) * sqrt(renderingEffect)

        x = offset.x + initialX + (initialX - center.x * 1f) / center.x *
                translationX * fraction * renderingEffect

        val firstRenderingFraction = time / 200f
        r = initialR * 1.2f
        if (firstRenderingFraction > 1.0) {
            val rTimeFraction = (time - 200f) / (lifeTime - 200f)
            r = min(15f, r * (1 - rTimeFraction))
        } else {
            val originalSize = max(r, initialR)
            r = originalSize - (originalSize - r) * firstRenderingFraction
        }
        alpha = (initialAlpha * min(1.2f - fraction, 1.0f)).toInt()
        return true
    }
}