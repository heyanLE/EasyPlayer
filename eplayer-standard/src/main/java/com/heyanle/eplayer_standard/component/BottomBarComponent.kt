package com.heyanle.eplayer_standard.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import com.heyanle.eplayer_core.constant.EasyPlayStatus
import com.heyanle.eplayer_core.controller.ComponentContainer
import com.heyanle.eplayer_core.controller.IComponent
import com.heyanle.eplayer_core.controller.IComponentGetter
import com.heyanle.eplayer_core.controller.IGestureComponent
import com.heyanle.eplayer_standard.databinding.ComponentBottomBarBinding
import com.heyanle.eplayer_standard.utils.TimeUtils

/**
 * Create by heyanlin on 2022/11/2
 */
class BottomBarComponent: FrameLayout, IGestureComponent, SeekBar.OnSeekBarChangeListener {

    private var container: ComponentContainer? = null

    private val binding: ComponentBottomBarBinding = ComponentBottomBarBinding.inflate(
        LayoutInflater.from(context), this, true)

    private var isLocked = false

    // seekbar 是否在滑动
    private var isSeekBarTouching = false

    // 是否在使用手势滑动进度
    private var isProgressSlide = false

    // 当前是否是可见状态（跟随 Controller 的状态，并不是真正的可不可见）
    private var isVisible = false

    private val showAnim = AlphaAnimation(0f, 1f).apply {
        duration = 300
        setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.root.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {

            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
    }

    private val hideAnim = AlphaAnimation(1f, 0f).apply {
        duration = 300
        setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.root.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.root.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
    }

    init {
        binding.seekBar.setOnSeekBarChangeListener(this)
        binding.ivFullscreen.setOnClickListener {
            runWithContainer {
                toggleFullScreen()
            }
        }
    }

    // == override IComponent

    override fun onPlayerStateChanged(playerState: Int) {
        TODO("Not yet implemented")
    }

    override fun onPlayStateChanged(playState: Int) {
        when(playState){
            EasyPlayStatus.STATE_PLAYING -> {
                runWithContainer {
                    startProgressUpdate()
                }
            }
            else -> {
                runWithContainer {
                    stopProgressUpdate()
                }
            }
        }
    }

    override fun onVisibleChanged(isVisible: Boolean) {
        this.isVisible = isVisible
        if(!isProgressSlide && !isSeekBarTouching){
            binding.root.clearAnimation()
            if(isVisible){
                binding.root.startAnimation(showAnim)
            }else{
                binding.root.startAnimation(hideAnim)
            }
        }
    }

    override fun onProgressUpdate(duration: Long, position: Long) {
        if(!isProgressSlide && !isSeekBarTouching){
            refreshTimeUI(duration, position)
            setSeekbarProgress(duration, position)
        }
    }

    override fun onLockStateChange(isLocked: Boolean) {
        this.isLocked = isLocked
    }

    override fun getView(): View {
        return this
    }

    override fun getLayoutParam(): RelativeLayout.LayoutParams {
        return RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
    }

    private fun requireContainer(): ComponentContainer {
        return container ?: throw NullPointerException()
    }

    private inline fun runWithContainer(block: ComponentContainer.()->Unit){
        container?.block()
    }

    override fun onAttachToContainer(container: ComponentContainer) {
        this.container = container
    }

    override fun onDetachToContainer(container: ComponentContainer) {
        this.container = null
    }

    // == override IGestureComponent


    override fun onSlidePositionChange(slidePosition: Long, currentPosition: Long, duration: Long) {
        super.onSlidePositionChange(slidePosition, currentPosition, duration)
        if(isLocked){
            return
        }
        isProgressSlide = true
        runWithContainer {
            stopFadeOut()
            stopProgressUpdate()
            binding.root.clearAnimation()
            binding.root.visibility = View.VISIBLE
            seekTo(slidePosition)
            refreshTimeUI(getDuration(), slidePosition)
            setSeekbarProgress(slidePosition, duration)
        }
    }

    override fun onStopSlide() {
        super.onStopSlide()
        isProgressSlide = false
        runWithContainer {
            startFadeOut()
            startProgressUpdate()

        }
    }

    // == override seekbar listener

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if(fromUser){
            runWithContainer {
                val newPosition = getDuration()/1000 * seekBar.progress / seekBar.max
                seekTo(newPosition)
                refreshTimeUI(getDuration(), newPosition)
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        isSeekBarTouching = true
        runWithContainer {
            stopFadeOut()
            stopProgressUpdate()
        }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        isSeekBarTouching = false
        runWithContainer {
            startFadeOut()
            startProgressUpdate()
            val newPosition = getDuration()/1000 * seekBar.progress / seekBar.max
            seekTo(newPosition)
        }
    }

    // == UI 显示效果控制

    private fun refreshTimeUI(duration: Long, position: Long){
        val durationStr = TimeUtils.toString(duration)
        val positionStr = TimeUtils.toString(position)
        binding.tvCurrentTime.text = durationStr
        binding.tvTotalTime.text = positionStr

    }

    private fun setSeekbarProgress(duration: Long, position: Long){
        binding.seekBar.progress = ((duration.toFloat()/position)*binding.seekBar.max).toInt()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)
}