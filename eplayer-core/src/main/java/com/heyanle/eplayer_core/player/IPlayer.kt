package com.heyanle.eplayer_core.player

import android.graphics.Bitmap

/**
 * 视频播放的一些 API 接口
 * Create by heyanlin on 2022/10/25
 */
interface IPlayer {

    fun start()

    fun pause()

    fun getDuration(): Long

    fun getCurrentPosition(): Long

    fun seekTo(pos: Long)

    fun isPlaying(): Boolean

    fun getBufferedPercentage(): Int

    fun startFullScreen()

    fun stopFullScreen()

    fun isFullScreen(): Boolean

    fun setMute(isMute: Boolean)

    fun isMute(): Boolean

    fun setScreenScaleType(screenScaleType: Int)

    fun setSpeed(speed: Float)

    fun getSpeed(): Float

    fun replay(resetPosition: Boolean)

    fun setMirrorRotation(enable: Boolean): Boolean

    fun doScreenShot(): Bitmap?

    fun getVideoSize(): IntArray

    fun setRotation(rotation: Float): Boolean

    fun startTinyScreen(): Boolean

    fun stopTinyScreen()

    fun isTinyScreen(): Boolean

}