package com.heyanle.eplayer_core.controller

/**
 * Create by heyanlin on 2022/10/25
 */
interface IGestureComponent: IComponent {

    /**
     * 开始长按 - 自动处理视频加速，这里只需要处理 UI
     */
    fun onLongPressStart()

    /**
     * 手指抬起 - 可用来取消长按视频加速的 UI
     */
    fun onUp()

    /**
     * 开始滑动
     */
    fun onStartSlide()

    /**
     * 结束滑动
     */
    fun onStopSlide()

    /**
     * 滑动调整进度
     * @param slidePosition 滑动进度
     * @param currentPosition 当前播放进度
     * @param duration 视频总长度
     */
    fun onPositionChange(slidePosition: Long, currentPosition: Long, duration: Long)

    /**
     * 滑动调整亮度
     * @param percent 亮度百分比
     */
    fun onBrightnessChange(percent: Int)

    /**
     * 滑动调整音量
     * @param percent 音量百分比
     */
    fun onVolumeChange(percent: Int)

}