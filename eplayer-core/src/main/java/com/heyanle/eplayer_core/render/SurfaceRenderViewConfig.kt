package com.heyanle.eplayer_core.render

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Create by heyanlin on 2022/10/27
 */
class SurfaceRenderViewConfig(context: Context?)
    : RenderViewConfig<SurfaceRenderFactory>(context) {


    override fun getFactory(): SurfaceRenderFactory {
        return SurfaceRenderFactory()
    }

}