package com.heyanle.eplayer_exo

import android.content.Context
import android.util.AttributeSet
import com.heyanle.eplayer_core.player.PlayerEngineVConfig

/**
 * Create by heyanlin on 2022/10/27
 */
class ExoPlayerEngineVConfig: PlayerEngineVConfig<ExoPlayerEngineFactory>  {
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

    override fun getFactory(): ExoPlayerEngineFactory {
        return ExoPlayerEngineFactory()
    }




}