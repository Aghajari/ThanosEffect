package com.aghajari.thanoseffect

import android.graphics.Point
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

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