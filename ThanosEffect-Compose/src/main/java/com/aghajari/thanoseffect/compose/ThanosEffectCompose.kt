package com.aghajari.thanoseffect.compose

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import com.aghajari.thanoseffect.ThanosEffect
import com.aghajari.thanoseffect.core.RenderConfigs
import com.aghajari.thanoseffect.widget.EffectedView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ThanosEffectCompose {

    internal var location: MutableState<Offset>? = null
    internal var graphicsLayer: GraphicsLayer? = null

    val state = mutableStateOf(State.NONE)

    suspend fun start(
        context: Context,
        pending: Int = 0,
        renderConfigs: RenderConfigs = RenderConfigs.default(),
    ) {
        graphicsLayer?.toImageBitmap()?.let { image ->
            if (state.value != State.NONE) {
                return
            }
            state.value = State.STARTING

            val view = image.asAndroidBitmap()
                .copy(Bitmap.Config.ARGB_8888, false)
                .asEffectedView(
                    offset = location?.value ?: Offset.Zero,
                )

            withContext(Dispatchers.Main) {
                ThanosEffect.start(
                    context = context,
                    effectedView = view,
                    pending = pending,
                    renderConfigs = renderConfigs,
                )
            }

            delay(100)
            if (state.value == State.STARTING) {
                state.value = State.STARTED
            }
        }
    }

    fun hasEffectStarted(): Boolean = state.value == State.STARTED
    fun hasDestroyed(): Boolean = state.value == State.DESTROYED

    fun clear() {
        state.value = State.NONE
    }

    fun destroy() {
        state.value = State.DESTROYED
        graphicsLayer = null
        location = null
    }

    private fun Bitmap.asEffectedView(offset: Offset) = object : EffectedView {

        override val width: Int = getWidth()
        override val height: Int = getHeight()
        override val translationX: Float = 0f
        override val translationY: Float = 0f

        override fun getLocationInWindow(location: IntArray) {
            location[0] = offset.x.toInt()
            location[1] = offset.y.toInt()
        }

        override fun createBitmap(): Bitmap {
            return this@asEffectedView
        }
    }

    enum class State {
        NONE,
        STARTING,
        STARTED,
        DESTROYED
    }
}