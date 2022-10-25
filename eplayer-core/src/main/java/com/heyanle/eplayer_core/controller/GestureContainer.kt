package com.heyanle.eplayer_core.controller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.heyanle.eplayer_core.constant.EasyPlayStatus
import com.heyanle.eplayer_core.constant.EasyPlayerStatus
import com.heyanle.eplayer_core.player.IPlayerEngine
import com.heyanle.eplayer_core.utils.PlayUtils
import kotlin.math.abs

/**
 * 在 BaseController 的基础上在实现手势控制
 * 1. 单击显示 / 隐藏控制器 UI
 * 2. 双击播放暂停
 * 3. 中间滑动控制进度
 * 4. 左边滑动控制声音
 * 5. 右边滑动控制亮度
 * 6. 长按倍速播放
 * Create by heyanlin on 2022/10/25
 */
class GestureContainer: BaseController,
    GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener,
    View.OnTouchListener {

    //手势分析器
    private var mGestureDetector: GestureDetector = GestureDetector(context, this)

    // 音乐控制器（用于获取和控制音量）
    private var mAudioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // 音量
    private var mStreamVolume = 0

    // 亮度
    private var mBrightness = 0f

    // 滑动的进度
    private var mSeekPosition = -1L

    // 手势事件分发要用到的一些标记
    private var mFirstTouch = false
    private var mChangePosition = false
    private var mChangeBrightness = false
    private var mChangeVolume = false
    private var mCanSlide = false
    private var mCanLongPress = false

    // 当前播放状态
    private var mCurPlayState = 0

    // 整个视频区域从最左划到最右滑过的视频时间
    var slideFullTime = 120000
    // 是否启动手势
    var isGestureEnabled = true
    // 是否启用滑动控制进度
    var canChangePosition = true
    // 是否在普通屏幕启动手势
    var enableSlideInNormal = false
    // 是否双击播放暂停
    var isDoubleTapTogglePlayEnabled = true

    // 是否启动长按倍速
    var isLongPressSpeedUp = true
    // 是否在普通屏幕启动长按倍速
    var enableLongPressInNormal = true

    // 播放器倍速控制回调，由外界实现
    // return: 是否显示相关 UI（将事件传递给 GestureComponent，默认不传递）
    var onPlayerSpeedUp:()->Boolean = {false}

    init {
        setOnTouchListener(this)
    }

    // == BaseController override ===========
    override fun dispatchPlayStateChange(playState: Int) {
        super.dispatchPlayStateChange(playState)
        mCurPlayState = playState
    }

    override fun dispatchPlayerStateChange(playerState: Int) {
        super.dispatchPlayerStateChange(playerState)
        if (playerState == EasyPlayerStatus.PLAYER_NORMAL) {
            mCanSlide = enableSlideInNormal
            mCanLongPress = enableLongPressInNormal
        } else if (playerState == EasyPlayerStatus.PLAYER_FULL_SCREEN) {
            mCanSlide = true
            mCanLongPress = true
        }
    }

    // == View override ===========


    // =========== 滑动事件处理 ===========================
    // == GestureDetector.OnGestureListener override ===========
    // == GestureDetector.OnDoubleTapListener

    /**
     * 手指按下的瞬间
     */
    override fun onDown(e: MotionEvent): Boolean {
        if (!isInPlaybackState() //不处于播放状态
            || !isGestureEnabled //关闭了手势
            || isEdge(e)
        ) //处于屏幕边沿
            return true
        mStreamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val activity: Activity = PlayUtils.findActivity(context) ?: return false
        mBrightness = activity.window?.attributes?.screenBrightness ?: 0f
        mFirstTouch = true
        mChangePosition = false
        mChangeBrightness = false
        mChangeVolume = false
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        if (isInPlaybackState()) {
            componentContainer?.toggleShowState()
        }
        return true
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        if (isDoubleTapTogglePlayEnabled && !isLocked() && isInPlaybackState()) togglePlay()
        return true
    }

    override fun onScroll(
        e1: MotionEvent,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        if (!isInPlaybackState() //不处于播放状态
            || !isGestureEnabled //关闭了手势
            || !mCanSlide //关闭了滑动手势
            || isLocked() //锁住了屏幕
            || isEdge(e1)
        ) //处于屏幕边沿
            return true
        val deltaX = e1.x - e2.x
        val deltaY = e1.y - e2.y
        if (mFirstTouch) {
            mChangePosition = abs(distanceX) >= abs(distanceY)
            if (!mChangePosition) {
                //半屏宽度
                if (e2.x > width/2f) {
                    mChangeVolume = true
                } else {
                    mChangeBrightness = true
                }
            }

            if (mChangePosition) {
                //根据用户设置是否可以滑动调节进度来决定最终是否可以滑动调节进度
                mChangePosition = canChangePosition
            }
            if (mChangePosition || mChangeBrightness || mChangeVolume) {
                runWithAllComponents {
                    (this as? IGestureComponent)?.onStartSlide()
                }
            }
            mFirstTouch = false

        }
        if (mChangePosition) {
            slideToChangePosition(deltaX)
        } else if (mChangeBrightness) {
            slideToChangeBrightness(deltaY)
        } else if (mChangeVolume) {
            slideToChangeVolume(deltaY)
        }
        return true
    }

    override fun onLongPress(e: MotionEvent?) {
        if (!isInPlaybackState() //不处于播放状态
            || !isLongPressSpeedUp //关闭了长按倍速
            || !mCanLongPress //不能长按
            || isLocked() //锁住了屏幕
        ) //处于屏幕边沿
            return
        onLongPress()
    }

    // 没用的事件
    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return false
    }


    // == View override ========
    // 处理手指 抬起
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //滑动结束时事件处理
        if (!mGestureDetector.onTouchEvent(event)) {
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopToSlide()
                    onUp()
                    if (mSeekPosition >= 0) {
                        componentContainer?.seekTo(mSeekPosition)
                        mSeekPosition = -1
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    stopToSlide()
                    onUp()
                    mSeekPosition = -1
                }
            }
        }
        return super.onTouchEvent(event)
    }

    // 将事件交给 Detector
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return mGestureDetector.onTouchEvent(event)
    }

    // == 事件处理和分发 ============

    private fun onLongPress(){
        if(onPlayerSpeedUp()){
            runWithAllComponents {
                (this as? IGestureComponent)?.onLongPressStart()
            }
        }
    }

    private fun slideToChangePosition(dx: Float) {
        var deltaX = dx
        deltaX = -deltaX
        val width = measuredWidth
        val duration = componentContainer?.getDuration()?:0L
        val currentPosition = componentContainer?.getCurrentPosition()?:0L
        var position = (deltaX / width * slideFullTime + currentPosition).toLong()
        if (position > duration) position = duration
        if (position < 0) position = 0

        runWithAllComponents {
            (this as? IGestureComponent)?.onPositionChange(position, currentPosition, duration)
        }

        mSeekPosition = position
    }

    private fun slideToChangeBrightness(deltaY: Float) {
        val activity: Activity = PlayUtils.findActivity(context) ?: return
        val window = activity.window
        val attributes = window.attributes
        val height = measuredHeight
        if (mBrightness == -1.0f) mBrightness = 0.5f
        var brightness = deltaY * 2 / height + mBrightness
        if (brightness < 0) {
            brightness = 0f
        }
        if (brightness > 1.0f) brightness = 1.0f
        val percent = (brightness * 100).toInt()
        attributes.screenBrightness = brightness
        window.attributes = attributes

        runWithAllComponents {
            (this as? IGestureComponent)?.onBrightnessChange(percent)
        }
    }

    private fun slideToChangeVolume(deltaY: Float) {
        val streamMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val height = measuredHeight
        val deltaV = deltaY * 2 / height * streamMaxVolume
        var index = mStreamVolume + deltaV
        if (index > streamMaxVolume) index = streamMaxVolume.toFloat()
        if (index < 0) index = 0f
        val percent = (index / streamMaxVolume * 100).toInt()
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index.toInt(), 0)
        runWithAllComponents {
            (this as? IGestureComponent)?.onVolumeChange(percent)
        }
    }

    private fun stopToSlide(){
        runWithAllComponents {
            (this as? IGestureComponent)?.onStopSlide()
        }
    }

    private fun onUp(){
        runWithAllComponents {
            (this as? IGestureComponent)?.onUp()
        }
    }

    // == utils =================

    // 是否在 控件边缘
    private fun isEdge(e: MotionEvent): Boolean{
        val edgeSize = PlayUtils.dp2px(context, 40f)
        return e.x < edgeSize || e.x > width - edgeSize
                ||e.y < edgeSize || e.y > height - edgeSize
    }

    // 是否正在播放（播放，暂停，缓冲）三种状态
    private fun isInPlaybackState(): Boolean {
        return componentContainer != null
                && mCurPlayState != EasyPlayStatus.STATE_ERROR
                && mCurPlayState != EasyPlayStatus.STATE_IDLE
                && mCurPlayState != EasyPlayStatus.STATE_PREPARING
                && mCurPlayState != EasyPlayStatus.STATE_PREPARED
                && mCurPlayState != EasyPlayStatus.STATE_START_ABORT
                && mCurPlayState != EasyPlayStatus.STATE_PLAYBACK_COMPLETED
    }

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)
}