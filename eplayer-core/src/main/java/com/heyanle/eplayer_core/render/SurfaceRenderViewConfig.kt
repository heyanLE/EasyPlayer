package com.heyanle.eplayer_core.render

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Create by heyanlin on 2022/10/27
 */
class SurfaceRenderViewConfig
    : RenderViewConfig<SurfaceRenderFactory> {
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


    override fun getFactory(): SurfaceRenderFactory {
        return SurfaceRenderFactory()
    }




}