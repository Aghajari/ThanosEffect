package com.aghajari.thanoseffect.opengl

import android.opengl.GLES31
import java.nio.ShortBuffer

internal class Handlers(
    drawProgram: Int,
) {

    private val timeHandle: Int = GLES31.glGetUniformLocation(drawProgram, "time")
    private val centerXHandle = GLES31.glGetUniformLocation(drawProgram, "centerX")
    private val centerYHandle = GLES31.glGetUniformLocation(drawProgram, "centerY")
    private val tyHandle = GLES31.glGetUniformLocation(drawProgram, "ty")
    private val txHandle = GLES31.glGetUniformLocation(drawProgram, "tx")
    private val renderingHandle = GLES31.glGetUniformLocation(drawProgram, "rendering")
    private val optimizedSizeHandle = GLES31.glGetUniformLocation(drawProgram, "optimizedSize")
    private val isRenderingBitmapHandle =
        GLES31.glGetUniformLocation(drawProgram, "isRenderingBitmap")
    private val viewSrcHandle = GLES31.glGetUniformLocation(drawProgram, "viewSrc")
    private val viewDstHandle = GLES31.glGetUniformLocation(drawProgram, "viewDst")
    private val viewTextureHandle = GLES31.glGetUniformLocation(drawProgram, "viewTexture")
    private val drawCircleHandle = GLES31.glGetUniformLocation(drawProgram, "drawCircle")

    fun updateTime(elapsedTime: Float) {
        GLES31.glUniform1f(timeHandle, elapsedTime)
    }

    fun drawTexture(
        texture: Int,
        tx: Float,
        ty: Float,
        srcNorm: FloatArray,
        dst: FloatArray,
        drawOrderBuffer: ShortBuffer?,
    ) {
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, 0)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture)
        GLES31.glUniform1i(viewTextureHandle, 0)
        GLES31.glUniform1f(txHandle, tx)
        GLES31.glUniform1f(tyHandle, ty)
        GLES31.glUniform1i(isRenderingBitmapHandle, 1)
        GLES31.glUniform4fv(viewSrcHandle, 1, srcNorm, 0)
        GLES31.glUniform4fv(viewDstHandle, 1, dst, 0)
        GLES31.glDrawElements(
            GLES31.GL_TRIANGLES,
            GLUtils.DRAW_ORDER.size,
            GLES31.GL_UNSIGNED_SHORT,
            drawOrderBuffer
        )
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, 0)
    }

    fun draw(
        buffer: Int,
        tx: Float,
        ty: Float,
        optimizedPerPx: Float,
        centerX: Float,
        centerY: Float,
        rendering: Float,
        drawCircle: Boolean,
        particleCount: Int,
    ) {
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, buffer)
        GLES31.glVertexAttribPointer(
            0,
            2,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            0
        ) // inRelativePosition (vec2)
        GLES31.glEnableVertexAttribArray(0)
        GLES31.glVertexAttribPointer(
            1,
            3,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            8
        ) // inColor (vec3)
        GLES31.glEnableVertexAttribArray(1)
        GLES31.glVertexAttribPointer(
            2,
            1,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            20
        ) // inAlpha (float)
        GLES31.glEnableVertexAttribArray(2)
        GLES31.glVertexAttribPointer(
            3,
            1,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            24
        ) // inLifetime (float)
        GLES31.glEnableVertexAttribArray(3)
        GLES31.glVertexAttribPointer(
            4,
            1,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            28
        ) // inTime (float)
        GLES31.glEnableVertexAttribArray(4)
        GLES31.glVertexAttribPointer(
            5,
            1,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            32
        ) // inScale (float)
        GLES31.glEnableVertexAttribArray(5)
        GLES31.glVertexAttribPointer(
            6,
            1,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            36
        ) // inVy (float)
        GLES31.glEnableVertexAttribArray(6)
        GLES31.glVertexAttribPointer(
            7,
            1,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            40
        ) // inDy (float)
        GLES31.glEnableVertexAttribArray(7)
        GLES31.glVertexAttribPointer(
            8,
            1,
            GLES31.GL_FLOAT,
            false,
            GLUtils.PARTICLE_BYTES,
            44
        ) // inDx (float)
        GLES31.glEnableVertexAttribArray(8)
        GLES31.glUniform1f(txHandle, tx)
        GLES31.glUniform1f(tyHandle, ty)
        GLES31.glUniform1f(optimizedSizeHandle, optimizedPerPx)
        GLES31.glUniform1f(centerXHandle, centerX)
        GLES31.glUniform1f(centerYHandle, centerY)
        GLES31.glUniform1f(renderingHandle, rendering)
        GLES31.glUniform1i(isRenderingBitmapHandle, 0)
        GLES31.glUniform1i(drawCircleHandle, if (drawCircle) 1 else 0)
        GLES31.glDrawArrays(GLES31.GL_POINTS, 0, particleCount)
    }
}