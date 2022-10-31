package com.heyanle.eplayer_core.player

import android.content.Context
import android.util.AttributeSet
import android.view.View

/**
 * Create by heyanlin on 2022/10/27
 */
abstract class PlayerEngineVConfig<T:IPlayerEngineFactory>: View, IPlayerEngineFactory {
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

    abstract fun getFactory(): T

    override fun invoke(p1: Context): IPlayerEngine {
        return getFactory().invoke(p1)
    }


}