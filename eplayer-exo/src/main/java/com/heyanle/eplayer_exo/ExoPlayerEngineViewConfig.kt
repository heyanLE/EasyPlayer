package com.heyanle.eplayer_exo

import android.content.Context
import com.heyanle.eplayer_core.player.PlayerEngineViewConfig

/**
 * Create by heyanlin on 2022/10/27
 */
class ExoPlayerEngineViewConfig(context: Context): PlayerEngineViewConfig<ExoPlayerEngineFactory>(context)  {
    override fun getFactory(): ExoPlayerEngineFactory {
        return ExoPlayerEngineFactory()
    }
}