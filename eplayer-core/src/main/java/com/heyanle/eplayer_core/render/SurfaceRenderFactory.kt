package com.heyanle.lib.render

import android.content.Context
import com.heyanle.eplayer_core.render.IRender
import com.heyanle.eplayer_core.render.IRenderFactory
import com.heyanle.eplayer_core.render.SurfaceRender

/**
 * Created by HeYanLe on 2022/10/23 15:52.
 * https://github.com/heyanLE
 */
class SurfaceRenderFactory: IRenderFactory {
    override fun createRender(context: Context): IRender {
        return SurfaceRender(context)
    }
}