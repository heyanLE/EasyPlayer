package com.heyanle.eplayer_core

import com.heyanle.eplayer_core.constant.ScreenScaleType
import com.heyanle.eplayer_core.player.IPlayerEngineFactory
import com.heyanle.eplayer_core.player.ProgressManager
import com.heyanle.eplayer_core.render.IRenderFactory

/**
 * 全局默认配置
 * Create by heyanlin on 2022/10/26
 */
object EasyPlayerManager {

    var playerEngineFactory: IPlayerEngineFactory? = null
    var renderFactory: IRenderFactory? = null

    var enableOrientation: Boolean = true
    var enableAudioFocus: Boolean = true

    var screenScaleType = ScreenScaleType.SCREEN_SCALE_ORIGINAL

    var progressManager: ProgressManager = ProgressManager.ofDefault()



}