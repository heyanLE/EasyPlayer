package com.heyanle.eplayer_core.render

import android.content.Context

/**
 * Created by HeYanLe on 2022/10/23 15:52.
 * https://github.com/heyanLE
 */
class SurfaceViewRenderFactory: IRenderFactory {

    override fun invoke(context: Context): IRender {
        return SurfaceViewRender(context)
    }
}