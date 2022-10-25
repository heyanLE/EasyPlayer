package com.heyanle.eplayer_core.player

import android.content.Context

/**
 * 音乐引擎生成器
 * Create by heyanlin on 2022/10/25
 */
interface IPlayerEngineFactory: (Context)->IPlayerEngine {
}