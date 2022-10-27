package com.heyanle.eplayer_exo

import android.content.Context
import com.heyanle.eplayer_core.player.IPlayerEngine
import com.heyanle.eplayer_core.player.IPlayerEngineFactory

/**
 * Created by HeYanLe on 2022/10/23 16:29.
 * https://github.com/heyanLE
 */
class ExoPlayerEngineFactory : IPlayerEngineFactory{
    override fun invoke(p1: Context): IPlayerEngine {
        return ExoPlayerEngine(p1)
    }
}