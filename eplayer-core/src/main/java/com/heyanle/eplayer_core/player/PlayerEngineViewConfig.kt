package com.heyanle.eplayer_core.player

import android.content.Context
import android.view.View

/**
 * Create by heyanlin on 2022/10/27
 */
abstract class PlayerEngineViewConfig<T:IPlayerEngineFactory>(context: Context): View(context), IPlayerEngineFactory {

    abstract fun getFactory(): T

    override fun invoke(p1: Context): IPlayerEngine {
        return getFactory().invoke(p1)
    }
}