package com.heyanle.eplayer_core.render

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Create by heyanlin on 2022/10/27
 */
abstract class RenderViewConfig<T: IRenderFactory>(context: Context?) : View(context), IRenderFactory {

    abstract fun getFactory(): T

    override fun invoke(p1: Context): IRender {
        return getFactory().invoke(p1)
    }

}