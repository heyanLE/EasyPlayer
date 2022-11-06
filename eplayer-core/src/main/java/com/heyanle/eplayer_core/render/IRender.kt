package com.heyanle.eplayer_core.render

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.heyanle.eplayer_core.controller.IController
import com.heyanle.eplayer_core.player.IPlayerEngine

/**
 * Created by HeYanLe on 2022/10/23 15:05.
 * https://github.com/heyanLE
 */
interface IRender {

    fun detachPlayerEngine(player: IPlayerEngine)

    fun attachToPlayerEngine(player: IPlayerEngine)

    fun setVideoSize(width: Int, height: Int)

    fun setVideoRotation(degree: Int)

    fun setScaleType(scaleType: Int)

    fun getView(): View

    // 可自行处理层级问题
    // renderContainer: getView 返回的 View 此时为 renderContainer 的唯一一个孩子
    // playerViewContainer：PlayerView 所有元素的父布局，此时 renderContainer 已经按照 ViewConfig 顺序添加到 ViewGroup
    // controllers：当前注册的 controllers 快照
    // 如果需要自行处理层级，需要先 rootViewContainer.removeView(renderContainer)
    // 然后自行处理层级
    fun onRenderLoad(renderContainer: ViewGroup, rootViewContainer: ViewGroup, controllers: LinkedHashSet<IController>) {

    }

    fun setBackgroundColor(color: Int)

    fun beforeAddToWindow(view: View, parent: FrameLayout)

    fun screenShot(): Bitmap?

    fun release()

}