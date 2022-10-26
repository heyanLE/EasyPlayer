package com.heyanle.eplayer_core.controller

import android.view.View
import android.view.animation.Animation
import android.widget.RelativeLayout

/**
 * 组件，注册给 Controller
 * Create by heyanlin on 2022/10/25
 */
interface IComponent: IComponentGetter {

    fun onPlayerStateChanged(playerState: Int)

    fun onPlayStateChanged(playState: Int)

    fun onVisibleChanged(isVisible: Boolean, anim: Animation)

    fun onProgressUpdate(duration: Long, position: Long)

    fun onLockStateChange(isLocked: Boolean)

    fun getView(): View?

    fun getLayoutParam(): RelativeLayout.LayoutParams?

    /**
     * 传入容器，组件可通过容器控制视频播放，或视频控制器
     * 可能会多次调用
     */
    fun onAttachToContainer(controller: ComponentContainer)

    /**
     * 移除时调用，建议在该方法里将 Container 对象置空，防止内存泄漏
     */
    fun onRemove()

    override fun getComponent(): IComponent {
        return this
    }

}