package com.heyanle.eplayer_core.easy_player

import android.content.Context
import com.heyanle.eplayer_core.player.IPlayerEngine
import com.heyanle.eplayer_core.player.IPlayerEngineFactory
import com.heyanle.eplayer_core.render.IRender
import com.heyanle.eplayer_core.render.IRenderFactory

/**
 * Create by heyanlin on 2022/10/26
 */
class EasyPlayerEnvironment(
    val playerEngine: IPlayerEngine,
    val render: IRender,
) {

    class Builder(
        var playerEngineFactory: IPlayerEngineFactory? = null,
        var renderFactory: IRenderFactory? = null
    ){

        fun build(context: Context): EasyPlayerEnvironment?{
            if(playerEngineFactory == null || renderFactory == null){
                return null
            }
            val playerEngine = playerEngineFactory?.invoke(context) ?: return null
            val renderFactory = renderFactory?.invoke(context) ?: return null
            return EasyPlayerEnvironment(playerEngine, renderFactory)
        }
    }



}