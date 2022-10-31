package com.heyanle.eplayer_core.render

import android.content.Context
import android.util.AttributeSet

/**
 * Create by heyanlin on 2022/10/31
 */
class TextureViewRenderVConfig : RenderVConfig<TextureViewRenderFactory> {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)


    override fun getFactory(): TextureViewRenderFactory {
        return TextureViewRenderFactory()
    }




}