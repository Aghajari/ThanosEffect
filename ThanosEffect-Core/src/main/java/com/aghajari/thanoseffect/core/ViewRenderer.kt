package com.aghajari.thanoseffect.core

interface ViewRenderer {

    var onFirstFrameRenderedCallback: (() -> Unit)?
    val weight: Int

    fun onNewFrameRendered() {
        onFirstFrameRenderedCallback?.let { callback ->
            callback.invoke()
            onFirstFrameRenderedCallback = null
        }
    }
}