package com.aghajari.thanoseffect.core

import kotlin.random.Random

/**
 * Interface for calculating the lifetime of a particle.
 * Provides properties and methods for generating a random lifetime for each
 * particle based on a maximum and minimum duration.
 */
interface ParticleLifeTime {

    /**
     * The maximum duration of a particle's lifetime in milliseconds.
     */
    val maxDuration: Int

    /**
     * The minimum duration of a particle's lifetime in milliseconds.
     */
    val minDuration: Int

    /**
     * The sensitivity of the particle's lifetime to its line index.
     */
    val lineSensitive: Int

    /**
     * Calculates the lifetime of a particle based on its line index and
     * the maximum number of lines.
     *
     * @param lineIndex The index of the line the particle belongs to.
     * @param maxLine The maximum number of lines.
     * @return The calculated lifetime of the particle in milliseconds.
     */
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