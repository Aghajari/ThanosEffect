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
import kotlinx.coroutines.withContext

/**
 * Class to manage and apply the Thanos effect in Jetpack Compose.
 */
class ThanosEffectCompose {

    internal var location: MutableState<Offset>? = null
    internal var graphicsLayer: GraphicsLayer? = null

    /**
     * The current state of the Thanos effect.
     */
    private val state = mutableStateOf(State.NONE)

    /**
     * Starts the Thanos effect.
     *
     * @param context The context in which the effect is applied.
     * @param pendingWeight The pending weight for the effect.
     * @param renderConfigs The configuration parameters for rendering the effect.
     */
    suspend fun start(
        context: Context,
        pendingWeight: Int = 0,
        renderConfigs: RenderConfigs = RenderConfigs.default(),
        onFirstFrameRenderedCallback: () -> Unit = {},
    ) {
        if (state.value != State.NONE) {
            return
        }

        graphicsLayer?.let { graphicsLayer ->
            state.value = State.STARTING

            val view = graphicsLayer.toImageBitmap()
                .asAndroidBitmap()
                .copy(Bitmap.Config.ARGB_8888, false)
                .asEffectedView(
                    offset = location?.value ?: Offset.Zero,
                )

            withContext(Dispatchers.Main) {
                ThanosEffect.start(
                    context = context,
                    effectedView = view,
                    pendingWeight = pendingWeight,
                    renderConfigs = renderConfigs,
                    onFirstFrameRenderedCallback = {
                        destroy()
                        onFirstFrameRenderedCallback.invoke()
                    }
                )
            }
        }
    }

    /**
     * Checks if the Thanos effect has started.
     *
     * @return `true` if the effect has started, `false` otherwise.
     */
    fun hasEffectStarted(): Boolean = state.value == State.DESTROYED

    /**
     * Destroys the Thanos effect, releasing resources.
     */
    private fun destroy() {
        state.value = State.DESTROYED
        graphicsLayer = null
        location = null
    }


    /**
     * Extension function to convert a [Bitmap] to an [EffectedView] with the given offset.
     *
     * @param offset The offset to apply to the view.
     * @return The converted [EffectedView].
     */
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

    private enum class State {
        NONE,
        STARTING,
        DESTROYED
    }
}