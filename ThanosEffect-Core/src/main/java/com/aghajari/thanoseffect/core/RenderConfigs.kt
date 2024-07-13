package com.aghajari.thanoseffect.core


data class RenderConfigs(
    val drawRectParticles: Boolean,
    val perPx: Int,
    val particleLifeTime: ParticleLifeTime,
    val lineDelay: Int,
) {

    companion object {

        fun default() = RenderConfigs(
            drawRectParticles = false,
            perPx = EffectUtils.calculateSize(),
            particleLifeTime = ParticleLifeTimeImpl,
            lineDelay = EffectUtils.getMaxAcceptableDelay().toInt() * 1000
        )
    }
}