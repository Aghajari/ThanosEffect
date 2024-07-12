package com.aghajari.thanoseffect

import kotlin.random.Random

interface ParticleLifeTime {

    val maxDuration: Int
    val minDuration: Int
    val lineSensitive: Int

    fun calculateLifeTime(lineIndex: Int, maxLine: Int): Int
}

internal object ParticleLifeTimeImpl : ParticleLifeTime {

    override val maxDuration: Int
        get() = 2800
    override val minDuration: Int
        get() = 1400
    override val lineSensitive: Int
        get() = 600

    override fun calculateLifeTime(lineIndex: Int, maxLine: Int): Int {
        val minus = ((maxLine - lineIndex).toFloat() / maxLine * lineSensitive).toInt()
        return minDuration + minus + Random.nextInt(maxDuration - minDuration - minus + 1)
    }
}