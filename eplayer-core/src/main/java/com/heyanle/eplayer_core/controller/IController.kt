package com.heyanle.eplayer_core.controller

import android.view.ViewGroup
import com.heyanle.eplayer_core.player.IPlayer
import com.heyanle.eplayer_core.player.IPlayerEngine

/**
 * 控制器接口
 * Create by heyanlin on 2022/10/25
 */
interface IController: IControllerGetter {

    /**
     * 绑定 IPlayer
     */
    fun attachToPlayer(playerController: IPlayer)

    /**
     * 解绑 IPlayer
     */
    fun detachPlayer(iPlayer: IPlayer)

    /**
     * 分发播放状态改变事件
     */
    fun dispatchPlayStateChange(playState: Int)

    /**
     * 分发播放器状态改变事件
     */
    fun dispatchPlayerStateChange(playerState: Int)

    /**
     * 分发返回按钮点击事件
     */
    fun onBackPressed(): Boolean


    fun startFadeOut()

    fun stopFadeOut()

    fun isShowing(): Boolean

    /**
     * 设置锁定状态
     */
    fun setLocked(locked: Boolean)

    /**
     * 是否在锁定状态
     */
    fun isLocked(): Boolean

    /**
     * 开始更新 Progress，开始后将会不断调用 Component 的 onProgressUpdate 方法
     */
    fun startProgressUpdate()

    /**
     * 停止更新 Progress
     */
    fun stopProgressUpdate()

    /**
     * 隐藏控制器
     */
    fun hide()

    /**
     * 展示控制器，并开始计时
     */
    fun show()

    /**
     * 获取 控制器 view 的 Container
     * 注意该方法可能会多次被多个对象调用，不要再里面初始化 View
     */
    fun getViewContainer(): ViewGroup


    /**
     * 添加组件
     * @param isAddToViewGroup 是否添加到 控制器 ViewGroup，会将 component.getView 添加到 container 中
     */
    fun addComponents(isAddToViewGroup: Boolean, vararg component: IComponent)

    /**
     * 移除组件
     */
    fun removeComponents(vararg component: IComponent)

    /**
     * 移除所有没有添加 view 的组件
     */
    fun removeAllComponentsWithoutAddToViewGroup()

    /**
     * 移除所有组件
     */
    fun removeAllComponents()

    override fun getController(): IController {
        return this
    }

}