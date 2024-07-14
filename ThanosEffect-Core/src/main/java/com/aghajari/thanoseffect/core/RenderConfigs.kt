package com.aghajari.thanoseffect.core

/**
 * Configuration parameters for rendering the Thanos effect.
 *
 * @property drawRectParticles Indicates whether to draw rectangular or circular particles.
 * @property perPx The number of pixels per particle. A higher value results in lower accuracy but boosts performance.
 * @property particleLifeTime Generates random lifetime for each particle based on a maximum and minimum value.
 * @property lineDelay The delay between rendering lines of particles.
 */
data class RenderConfigs(
    val drawRectParticles: Boolean,
    val perPx: Int,
    val particleLifeTime: ParticleLifeTime,
    val lineDelay: Int,
) {

    companion object {

        /**
         * Creates a default instance of [RenderConfigs].
         *
         * @return A [RenderConfigs] instance with default values.
         */
        fun default() = RenderConfigs(
            drawRectParticles = false,
            perPx = EffectUtils.calculateSize(),
            particleLifeTime = ParticleLifeTimeImpl,
            lineDelay = EffectUtils.getMaxAcceptableDelay().toInt() * 1000
        )
    }
}