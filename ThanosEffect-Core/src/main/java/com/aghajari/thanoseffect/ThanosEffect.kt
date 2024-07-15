package com.aghajari.thanoseffect

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.aghajari.thanoseffect.core.ThanosEffectTextureView
import com.aghajari.thanoseffect.core.EffectUtils
import com.aghajari.thanoseffect.core.RenderConfigs
import com.aghajari.thanoseffect.widget.EffectedView
import com.aghajari.thanoseffect.widget.asEffectedView

object ThanosEffect {

    /**
     * Renderer for the Thanos effect.
     */
    var renderer: ThanosEffectRenderer? = null

    private var textureView: ThanosEffectTextureView<*>? = null

    /**
     * Starts the Thanos effect on a given view.
     *
     * @param view The view to apply the effect on.
     * @param pendingWeight The pending weight of the view.
     * @param autoDelete Whether to automatically delete the view after applying the effect.
     * @param renderConfigs The configuration parameters for rendering the effect.
     * @param onFirstFrameRenderedCallback Calls once the first frame rendered.
     * @throws NullPointerException If the renderer is not set.
     */
    fun start(
        view: View,
        pendingWeight: Int = 0,
        autoDelete: Boolean = true,
        renderConfigs: RenderConfigs = RenderConfigs.default(),
        onFirstFrameRenderedCallback: () -> Unit = {},
    ) {
        initialize(view.context)
        val firstRenderCallback = if (autoDelete) {
            {
                (view.parent as? ViewGroup)?.removeView(view)
                onFirstFrameRenderedCallback.invoke()
            }
        } else onFirstFrameRenderedCallback

        textureView?.attach(
            view.asEffectedView(),
            pendingWeight,
            renderConfigs,
            firstRenderCallback
        )
    }

    /**
     * Starts the Thanos effect on a given effected view.
     *
     * @param context The context in which the view exists.
     * @param effectedView The view to apply the effect on.
     * @param pendingWeight The pending weight of the view.
     * @param renderConfigs The configuration parameters for rendering the effect.
     * @param onFirstFrameRenderedCallback Calls once the first frame rendered.
     * @throws NullPointerException If the renderer is not set.
     */
    fun start(
        context: Context,
        effectedView: EffectedView,
        pendingWeight: Int = 0,
        renderConfigs: RenderConfigs = RenderConfigs.default(),
        onFirstFrameRenderedCallback: () -> Unit = {}
    ) {
        initialize(context)
        textureView?.attach(
            effectedView,
            pendingWeight,
            renderConfigs,
            onFirstFrameRenderedCallback
        )
    }

    /**
     * Pauses the Thanos effect rendering.
     */
    fun pause() {
        textureView?.pause(true)
    }

    /**
     * Resumes the Thanos effect rendering.
     */
    fun resume() {
        textureView?.pause(false)
    }

    /**
     * Destroys the Thanos effect, releasing resources.
     */
    fun destroy() {
        textureView?.forceDestroy()
        destroyTextureView()
    }

    internal fun destroyTextureView() {
        EffectUtils.runOnUIThread {
            textureView?.let {
                renderer?.removeTextureView(it)
            }
            textureView = null
        }
    }

    /**
     * Initializes the Thanos effect by setting up the texture view.
     *
     * @param context The context in which to initialize the effect.
     */
    private fun initialize(context: Context) {
        if (renderer == null) {
            throw NullPointerException("You must set ThanosEffect.renderer before using it.")
        }

        if (textureView == null) {
            val activity = EffectUtils.findActivity(context)
            val rootView = activity?.findViewById<View>(android.R.id.content)
                ?.rootView as? ViewGroup
            if (rootView != null) {
                EffectUtils.init(context)
                textureView = renderer!!.createTextureView(rootView)
            }
        }
    }
}