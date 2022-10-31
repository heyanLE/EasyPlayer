package com.heyanle.eplayer_core.easy_player

import android.content.Context
import com.heyanle.eplayer_core.EasyPlayerManager
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
            if(playerEngineFactory == null){
                if(EasyPlayerManager.playerEngineFactory == null){
                    return null
                }
                playerEngineFactory = EasyPlayerManager.playerEngineFactory
            }
            if(renderFactory == null){
                if(EasyPlayerManager.renderFactory == null){
                    return null
                }
                renderFactory = EasyPlayerManager.renderFactory
            }
            val playerEngine = playerEngineFactory?.invoke(context) ?: return null
            val renderFactory = renderFactory?.invoke(context) ?: return null
            return EasyPlayerEnvironment(playerEngine, renderFactory)
        }
    }



}