package com.heyanle.easyplayer.component

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import com.heyanle.easyplayer.R
import com.heyanle.easyplayer.databinding.TestComponentBinding
import com.heyanle.easyplayer.utils.TimeUtils
import com.heyanle.eplayer_core.constant.EasyPlayStatus
import com.heyanle.eplayer_core.constant.EasyPlayerStatus
import com.heyanle.eplayer_core.controller.ComponentContainer
import com.heyanle.eplayer_core.controller.IComponentGetter
import com.heyanle.eplayer_core.controller.IGestureComponent

/**
 * Create by heyanlin on 2022/10/28
 */
class TestComponent: FrameLayout, IGestureComponent, IComponentGetter, SeekBar.OnSeekBarChangeListener {

    private var container: ComponentContainer? = null
    private val binding: TestComponentBinding by lazy {
        TestComponentBinding.inflate(LayoutInflater.from(context))
    }

    // seekbar 是否在滑动
    private var isSeekBarTouching = false

    private val showAnim: Animation = AlphaAnimation(0f, 1f).apply {
        duration = 300
        setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.bottomLayout.visibility = View.VISIBLE
                binding.ivController.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.bottomLayout.visibility = View.VISIBLE
                binding.ivController.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }
        })
    }

    private val hideAnim: Animation = AlphaAnimation(1f, 0f).apply {
        duration = 300
        setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                binding.bottomLayout.visibility = View.VISIBLE
                binding.ivController.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animation?) {
                binding.bottomLayout.visibility = View.GONE
                binding.ivController.visibility = View.GONE
            }

            override fun onAnimationRepeat(animation: Animation?) {

            }
        })
    }

    init {
        addView(binding.root)
        binding.seekBar.setOnSeekBarChangeListener(this)
        binding.ivFullscreen.setOnClickListener{
            container?.toggleFullScreen()
        }
    }

    // == override IComponent ========================

    override fun onPlayerStateChanged(playerState: Int) {
        when(playerState){
            EasyPlayerStatus.PLAYER_NORMAL -> {
                toast("普通")
            }
            EasyPlayerStatus.PLAYER_FULL_SCREEN -> {
                toast("全屏")
            }
        }
    }

    override fun onPlayStateChanged(playState: Int) {
        when(playState){
            EasyPlayStatus.STATE_PLAYING -> {
                binding.progressBar.visibility = View.GONE
                container?.startProgressUpdate()
                binding.ivController.setImageResource(R.drawable.ic_baseline_pause_24)
            }
            EasyPlayStatus.STATE_BUFFERED -> {
                binding.progressBar.visibility = View.GONE
            }
            EasyPlayStatus.STATE_PAUSED -> {
                binding.progressBar.visibility = View.GONE
                container?.stopProgressUpdate()
                binding.ivController.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }
            EasyPlayStatus.STATE_BUFFERING -> {
                container?.stopProgressUpdate()
                binding.progressBar.visibility = View.VISIBLE
            }
        }
    }

    override fun onVisibleChanged(isVisible: Boolean) {
        //Animation.
        if(isVisible){
            binding.ivController.startAnimation(showAnim)
            binding.bottomLayout.startAnimation(showAnim)
        }else {
            binding.ivController.startAnimation(hideAnim)
            binding.bottomLayout.startAnimation(hideAnim)
        }
    }

    override fun onProgressUpdate(duration: Long, position: Long) {
        Log.d("TestComponent", "duration -> $duration position -> $position")
        binding.tvCurrentTime.text = TimeUtils.toString(position/1000)
        binding.tvTotalTime.text = TimeUtils.toString(duration/1000)

    }

    override fun onLockStateChange(isLocked: Boolean) {

    }

    override fun getView(): View {
        return this
    }

    override fun getLayoutParam(): RelativeLayout.LayoutParams? {
        return null
    }

    override fun onAttachToContainer(controller: ComponentContainer) {
        this.container = controller
    }

    override fun onDetachToContainer(controller: ComponentContainer) {
        this.container = null
    }

    override fun onRemove() {

    }

    // == override IGestureComponent ===============

    override fun onLongPressStart() {
        container?.setSpeed(3.0f)
        toast("LongPress")
    }

    override fun onUp() {
        container?.setSpeed(1.0f)
        toast("onUp")
        container?.startFadeOut()
        container?.startProgressUpdate()
    }

    override fun onStartSlide() {

    }

    override fun onStopSlide() {

    }

    override fun onPositionChange(slidePosition: Long, currentPosition: Long, duration: Long) {
        container?.stopFadeOut()
        container?.stopProgressUpdate()
        binding.bottomLayout.visibility = View.VISIBLE
        binding.tvCurrentTime.text = TimeUtils.toString(slidePosition/1000)
        binding.tvTotalTime.text = TimeUtils.toString(duration/1000)
        changeSeekbar(slidePosition/1000, duration/1000)

    }

    override fun onBrightnessChange(percent: Int) {
        toast("bright change $percent")
    }

    override fun onVolumeChange(percent: Int) {
        toast("volume change $percent")
    }

    // == override seekbar.OnSeekBarChangeListener

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        container?.let {
            if(fromUser){
                val pos = ((progress.toFloat()/seekBar.max)*it.getDuration()/1000).toLong()
                container?.seekTo(pos.toLong())
                binding.tvCurrentTime.text = TimeUtils.toString(pos)
            }
        }

    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        isSeekBarTouching = true
        container?.stopFadeOut()
        container?.stopProgressUpdate()

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        isSeekBarTouching = false
        container?.let {
            it.startFadeOut()
            it.startProgressUpdate()
            val newPosition = it.getDuration()/1000 * seekBar.progress / seekBar.max
            it.seekTo(newPosition)
        }
    }

    private fun changeSeekbar(position: Long, duration: Long){
        val progress = ((position.toFloat()*binding.seekBar.max)/duration).toInt()
        binding.seekBar.progress = progress
    }

    private fun toast(msg: String){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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