package com.heyanle.eplayer_core.render

import android.content.Context

/**
 * Create by heyanlin on 2022/10/31
 */
class TextureViewRenderFactory: IRenderFactory {

    override fun invoke(context: Context): IRender {
        return TextureViewRender(context)
    }
}