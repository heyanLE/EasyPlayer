package com.heyanle.eplayer_core.render

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import com.heyanle.eplayer_core.player.IPlayerEngine

/**
 * Created by HeYanLe on 2022/10/23 15:05.
 * https://github.com/heyanLE
 */
interface IRender {

    fun attachToPlayer(player: IPlayerEngine)

    fun setVideoSize(width: Int, height: Int)

    fun setVideoRotation(degree: Int)

    fun setScaleType(scaleType: Int)

    fun getView(): View

    fun beforeAddToWindow(view: View, parent: ViewGroup)

    fun screenShot(): Bitmap?

    fun release()

}