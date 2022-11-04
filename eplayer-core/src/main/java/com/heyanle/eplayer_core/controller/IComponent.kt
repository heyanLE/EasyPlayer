package com.heyanle.eplayer_core.controller

import android.view.View
import android.view.animation.Animation
import android.widget.RelativeLayout

/**
 * 组件，注册给 Controller
 * Create by heyanlin on 2022/10/25
 */
interface IComponent: IComponentGetter {

    fun onPlayerStateChanged(playerState: Int){}

    fun onPlayStateChanged(playState: Int){}

    fun onVisibleChanged(isVisible: Boolean){}

    fun onProgressUpdate(duration: Long, position: Long){}

    fun onLockStateChange(isLocked: Boolean){}

    /**
     * 获取 View，如果为 null 则不添加到 Controller 的 Container 中，只调用其他监听事件
     * 否则则将其添加进 Controller 的 Container 中
     * 注意该方法可能会多次被多个对象调用，不要再里面初始化 View
     */
    fun getView(): View?

    fun getLayoutParam(): RelativeLayout.LayoutParams? = null

    /**
     * 传入容器，组件可通过容器控制视频播放，或视频控制器
     * 注意该方法可能会多次被多个对象调用，不要再里面进行其他操作，直接更新持有的 ComponentContainer 对象即可
     */
    fun onAttachToContainer(container: ComponentContainer)

    fun onDetachToContainer(container: ComponentContainer)

    /**
     * 移除时调用，建议在该方法里将 Container 对象置空，防止内存泄漏
     */
    fun onRemove(){}

    override fun getComponent(): IComponent {
        return this
    }

}