package com.heyanle.eplayer_core.easy_player

import android.app.Activity
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.graphics.Color
import android.media.AudioManager
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media.AudioAttributesCompat
import com.heyanle.eplayer_core.EasyPlayerManager
import com.heyanle.eplayer_core.constant.EasyPlayStatus
import com.heyanle.eplayer_core.constant.EasyPlayerStatus
import com.heyanle.eplayer_core.constant.OtherPlayerEvent
import com.heyanle.eplayer_core.controller.IController
import com.heyanle.eplayer_core.controller.IControllerGetter
import com.heyanle.eplayer_core.player.IPlayer
import com.heyanle.eplayer_core.player.IPlayerEngine
import com.heyanle.eplayer_core.player.PlayerEngineVConfig
import com.heyanle.eplayer_core.render.RenderVConfig
import com.heyanle.eplayer_core.utils.ActivityScreenHelper
import com.heyanle.eplayer_core.utils.AudioFocusHelper
import com.heyanle.eplayer_core.utils.MediaHelper
import com.heyanle.eplayer_core.utils.PlayUtils
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Create by heyanlin on 2022/10/26
 */
open class BaseEasyPlayerView:
    FrameLayout,
    IPlayer,
    IPlayerEngine.EventListener,
    AudioFocusHelper.OnAudioFocusListener
{

    private val realViewContainer = FrameLayout(context)

    private val renderContainer = FrameLayout(context)

    // 环境
    private val realEnvironment = AtomicReference<EasyPlayerEnvironment?>(null)

    // 环境创建器
    private var environmentBuilder = EasyPlayerEnvironment.Builder()

    // controller 容器和锁
    protected val controllers: LinkedHashSet<IController> = LinkedHashSet()
    protected val controllerLock = ReentrantReadWriteLock()

    // 媒体源
    protected var url: String = ""
    protected var headers: Map<String, String> = hashMapOf()

    protected var fd: AssetFileDescriptor? = null

    // 当前播放状态和播放器状态
    private var mCurrentPlayerState = EasyPlayerStatus.PLAYER_NORMAL
    private var mCurrentPlayState = EasyPlayStatus.STATE_IDLE

    // 当前进度（临时变量）
    private var mCurrentPosition: Long = 0L
    var progressManager = EasyPlayerManager.progressManager

    // 是否循环播放
    var isLooping: Boolean = false
    // 是否静音
    private var mIsMute: Boolean = false

    // 当前缩放情况
    private var mCurrentScaleType: Int = EasyPlayerManager.screenScaleType

    // 音量焦点
    protected var audioFocusHelper: AudioFocusHelper = AudioFocusHelper(context)
    var enableAudioFocus = EasyPlayerManager.enableAudioFocus

    protected var mVideoSize = intArrayOf(0, 0)

    private var mIsFullScreen = false

    private var videoBackgroundColor = EasyPlayerManager.backgroundColor

    init {
        realViewContainer.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        addView(realViewContainer, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT ))
        // realViewContainer.addView(renderContainer, 0, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT ))
        audioFocusHelper.setListener(this)

    }

    fun setDataSource(url: String, headers:  Map<String, String>? = null){
        this.url = url
        headers?.let {
            this.headers = it
        }

    }

    // == Controller 管理和 事件分发 ============

    fun addController(isAddToParent: Boolean = true, vararg controller: IController){
        controllerLock.write {
            for(c in controller){
                controllers.add(c)
                c.attachToPlayer(this)
                if(isAddToParent){
                    realViewContainer.addView(c.getViewContainer())
                }
            }
        }
    }

    fun removeController(vararg controller: IController) {
        controllerLock.write {
            for(c in controller){
                controllers.remove(c)
                c.detachPlayer(this)
                realViewContainer.removeView(c.getViewContainer())
            }
        }
    }

    private fun getControllersSnapshot(): LinkedHashSet<IController> {
        val res = LinkedHashSet<IController>()
        controllerLock.read {
            res.addAll(controllers)
        }
        return res
    }

    protected fun dispatchPlayerStateChange(playerState: Int){
        mCurrentPlayerState = playerState
        runWithController {
            dispatchPlayerStateChange(playerState)
        }
    }

    protected fun dispatchPlayStateChange(playState: Int){
        mCurrentPlayState = playState
        runWithController {
            dispatchPlayStateChange(playState)
        }
    }

    protected inline fun runWithController(block: IController.()->Unit){
        controllerLock.read {
            controllers.iterator().forEach {
                it.block()
            }
        }
    }

    // == override IPlayer ==========================

    override fun start() {
        post {
            if (isInIdleState()
                || isInStartAbortState()
            ) {
                startFirst()
            } else if (isInPlaybackState()) {
                startInPlaybackState()
            }
        }

    }

    override fun pause() {
        post {
            runWithEnvironmentIfNotNull {
                if(isInPlaybackState() && playerEngine.isPlaying()){
                    playerEngine.pause()
                    dispatchPlayStateChange(EasyPlayStatus.STATE_PAUSED)
                    audioFocusHelper.abandonFocus()
                    renderContainer.keepScreenOn = false
                }

            }
        }

    }

    override fun getDuration(): Long {
        if (isInPlaybackState()) {
            runWithEnvironmentIfNotNull {
                return playerEngine.getDuration()
            }
            return 0
        } else return 0
    }

    override fun getCurrentPosition(): Long {
        if(isInPlaybackState()){
            runWithEnvironmentIfNotNull {
                mCurrentPosition = playerEngine.getCurrentPosition()?:0L
                return mCurrentPosition
            }
        }
        return 0
    }

    override fun seekTo(pos: Long) {
        if(isInPlaybackState()){
            runWithEnvironmentIfNotNull {
                playerEngine.seekTo(pos)
            }
        }
    }

    override fun isPlaying(): Boolean {
        runWithEnvironmentIfNotNull {
            return isInPlaybackState() && playerEngine.isPlaying()
        }
        return false
    }

    override fun getBufferedPercentage(): Int {
        runWithEnvironmentIfNotNull {
            return playerEngine.getBufferedPercentage()
        }
        return 0
    }

    override fun startFullScreen(changeScreen: Boolean) {
        if(mIsFullScreen){
            return
        }
        runWithEnvironmentIfNotNull {
            val activity = getActivity()?: return
            val decorView = (activity.window.decorView as? ViewGroup)?: return
            mIsFullScreen = true

            if(changeScreen) {
                // 横屏
                ActivityScreenHelper.activityScreenOrientationLandscape(activity)
            }

            // 移除视图
            removeView(realViewContainer)

            // 添加 decorView
            decorView.addView(realViewContainer)

            // 分发事件
            dispatchPlayerStateChange(EasyPlayerStatus.PLAYER_FULL_SCREEN)

            // 隐藏状态栏和虚拟按键
            MediaHelper.setIsSystemBarsShow(activity, false)
            MediaHelper.setSystemBarsBehavior(activity, WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        }
    }

    override fun stopFullScreen(changeScreen: Boolean) {
        if(!mIsFullScreen) return
        val activity = getActivity()?: return
        val decorView = (activity.window.decorView as? ViewGroup)?: return
        mIsFullScreen = false
        if(changeScreen) {
            // 竖屏
            ActivityScreenHelper.activityScreenOrientationPortrait(activity)
        }

        // 从  decorView 中移除
        decorView.removeView(realViewContainer)

        // 添加到自身
        addView(realViewContainer)

        // 分发事件
        dispatchPlayerStateChange(EasyPlayerStatus.PLAYER_NORMAL)

        // 展示状态栏和虚拟按钮
        MediaHelper.setIsSystemBarsShow(activity, true)

    }

    override fun isFullScreen(): Boolean {
        return mIsFullScreen
    }

    override fun setMute(isMute: Boolean) {
        mIsMute = isMute
        runWithEnvironmentIfNotNull {
        val volume = if(isMute)0f else 1f
            playerEngine.setVolume(volume, volume)
        }
    }

    override fun isMute(): Boolean {
        return mIsMute
    }

    override fun setScreenScaleType(screenScaleType: Int) {
        mCurrentScaleType = screenScaleType
        runWithEnvironmentIfNotNull {
            render.setScaleType(screenScaleType)
        }
    }

    override fun setSpeed(speed: Float) {
        if(isInPlaybackState()){
            runWithEnvironmentIfNotNull {
                playerEngine.setSpeed(speed)
            }
        }
    }

    override fun getSpeed(): Float {
        if(isInPlaybackState()){
            runWithEnvironmentIfNotNull {
                return playerEngine.getSpeed()
            }
            return 1f
        }
        return 1f
    }

    override fun replay(resetPosition: Boolean) {
        if(resetPosition){
            mCurrentPosition = 0
            startPrepare(true)
        }
    }

    override fun setMirrorRotation(enable: Boolean) {
        runWithEnvironmentIfNotNull {
            render.getView().scaleX = if(enable) -1f else 1f
        }
    }

    override fun doScreenShot(): Bitmap? {
        runWithEnvironmentIfNotNull {
            return render.screenShot()
        }
        return null
    }

    override fun getVideoSize(): IntArray {
        return mVideoSize
    }

    override fun setPlayerRotation(rotation: Float) {
        runWithEnvironmentIfNotNull {
            render.setVideoRotation(rotation.toInt())
        }
    }

    override fun startTinyScreen(): Boolean {
        // TODO 之后支持
        return false
    }

    override fun stopTinyScreen() {

    }

    override fun isTinyScreen(): Boolean {
        return false
    }

    // == override IPlayer.Listener =================
    override fun onError() {
        renderContainer.keepScreenOn = false
        dispatchPlayStateChange(EasyPlayStatus.STATE_ERROR)
    }

    override fun onCompletion() {
        renderContainer.keepScreenOn = false
        dispatchPlayStateChange(EasyPlayStatus.STATE_PLAYBACK_COMPLETED)
    }

    override fun onPrepared() {
        dispatchPlayStateChange(EasyPlayStatus.STATE_PREPARED)
        requestFocusIfNeed()
        if(mCurrentPosition > 0){
            seekTo(mCurrentPosition)
        }
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        mVideoSize[0] = width
        mVideoSize[1] = height

        runWithEnvironmentIfNotNull {
            render.setScaleType(mCurrentScaleType)
            render.setVideoSize(width, height)
        }
    }

    override fun onOtherPlayerEvent(event: Int, vararg args: Any) {
        when(event){
            OtherPlayerEvent.OTHER_PLAYER_EVENT_BUFFERING_START -> dispatchPlayStateChange(EasyPlayStatus.STATE_BUFFERING)
            OtherPlayerEvent.OTHER_PLAYER_EVENT_BUFFERING_END -> dispatchPlayStateChange(EasyPlayStatus.STATE_BUFFERED)
            OtherPlayerEvent.OTHER_PLAYER_EVENT_RENDERING_START -> {
                dispatchPlayStateChange(EasyPlayStatus.STATE_PLAYING)
                renderContainer.keepScreenOn = true
            }
            OtherPlayerEvent.OTHER_PLAYER_EVENT_VIDEO_ROTATION_CHANGED -> {
                val rotation = (args[0] as? Int )?:return
                runWithEnvironmentIfNotNull {
                    render.setVideoRotation(rotation)
                }
            }
        }
    }

    // == override audioFocusHelper.OnAudioFocusChangeListener ==========
    private var mStartRequested: Boolean = false
    private var mPausedForLoss = false
    override fun onAudioFocusChange(focusChange: Int) {
        when(focusChange){
            AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT -> {
                if (mStartRequested || mPausedForLoss) {
                    start()
                    mStartRequested = false
                    mPausedForLoss = false
                }
                if (!isMute()) //恢复音量
                    runWithEnvironmentIfNotNull {
                        playerEngine.setVolume(1.0f, 1.0f)
                    }

            }
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (isPlaying()) {
                    mPausedForLoss = true
                    pause()
                }
            }

            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                runWithEnvironmentIfNotNull {
                    if (playerEngine.isPlaying() && isMute()) {
                        playerEngine.setVolume(0.1f, 0.1f)
                    }
                }

            }
        }
    }

    // == PlayerView 自带相关逻辑 ===============

    fun release(){
        if (!isInIdleState()) {
            runWithEnvironmentIfNotNull {
                playerEngine.release()
                render.release()
            }

            //释放Assets资源
            kotlin.runCatching {
                fd?.close()
            }.onFailure {
                it.printStackTrace()
            }
            //关闭AudioFocus监听
            audioFocusHelper.abandonFocus()
            //关闭屏幕常亮
            renderContainer.keepScreenOn = false

            saveProgress()
            //重置播放进度
            mCurrentPosition = 0
            //切换转态
            dispatchPlayStateChange(EasyPlayStatus.STATE_IDLE)
        }
    }


    protected open fun startFirst(): Boolean{
        if(url.isNotEmpty() ){
            mCurrentPosition = progressManager.getProgress(url)
        }
        startPrepare()
        requestFocusIfNeed()
        val environment = requireEnvironment()
        environment.playerEngine.start()
        dispatchPlayStateChange(EasyPlayStatus.STATE_PLAYING)
        renderContainer.keepScreenOn = true

        return true
    }

    protected open fun startInPlaybackState(){
        requestFocusIfNeed()
        val environment = requireEnvironment()
        environment.playerEngine.start()
        dispatchPlayStateChange(EasyPlayStatus.STATE_PLAYING)
        renderContainer.keepScreenOn = true
    }

    protected open fun setPlayerEngineConfig(){
        val environment = requireEnvironment()
        environment.playerEngine.setLooping(isLooping)
    }

    protected open fun startPrepare(reset: Boolean = false){
        // 顺便检查环境
        val environment = requireEnvironment()
        if(reset){
            environment.playerEngine.reset()
            setPlayerEngineConfig()
        }
        if(prepareDataSource()) {
            environment.playerEngine.prepareAsync()
            dispatchPlayerStateChange(
                if(isFullScreen()) EasyPlayerStatus.PLAYER_FULL_SCREEN
                else if(isTinyScreen()) EasyPlayerStatus.PLAYER_TINY_SCREEN
                else EasyPlayerStatus.PLAYER_NORMAL)
            dispatchPlayStateChange(EasyPlayStatus.STATE_PREPARED)
        }
    }

    protected open fun prepareDataSource(): Boolean {
        val environment = requireEnvironment()
        if (fd != null) {
            environment.playerEngine.setVideoSource(fd!!)
            return true
        } else if (!TextUtils.isEmpty(url)) {
            environment.playerEngine.setVideoSource(url, headers)
            return true
        }
        return false
    }

    // 状态判断
    protected open fun isInPlaybackState(): Boolean {
        return realEnvironment.get()?.playerEngine != null &&
                mCurrentPlayState != EasyPlayStatus.STATE_ERROR &&
                mCurrentPlayState != EasyPlayStatus.STATE_IDLE &&
                mCurrentPlayState != EasyPlayStatus.STATE_PREPARING &&
                mCurrentPlayState != EasyPlayStatus.STATE_START_ABORT &&
                mCurrentPlayState != EasyPlayStatus.STATE_PLAYBACK_COMPLETED
    }

    protected open fun isInIdleState(): Boolean{
        return mCurrentPlayState == EasyPlayStatus.STATE_IDLE
    }

    protected open fun isInStartAbortState(): Boolean {
        return mCurrentPlayState == EasyPlayStatus.STATE_START_ABORT
    }

    // 加载 environmentBuilder 中的环境
    fun loadEnvironment(){
        val old = realEnvironment.get()
        val new = environmentBuilder.build(context) ?: throw IllegalStateException("EasyPlayerView environment build failed !")
        if(realEnvironment.compareAndSet(old, new)){
            old?.playerEngine?.let {
                it.removeEventListener(this)
                it.release()
            }
            old?.render?.let {
                it.detachPlayerEngine(old.playerEngine)
                it.release()
                renderContainer.removeView(it.getView())
            }

            new.playerEngine.setEventListener(this)
            new.render.attachToPlayerEngine(new.playerEngine)
            new.render.beforeAddToWindow(new.render.getView(), renderContainer)
            new.render.onRenderLoad(renderContainer, realViewContainer, getControllersSnapshot())
            renderContainer.addView(new.render.getView())
            new.playerEngine.init()
            realViewContainer.setBackgroundColor(videoBackgroundColor)
        }
    }

    private fun findEnvironmentAndControllerFromChildren(){
        val viewList = ArrayList<View>()
        for(i in 0 until childCount){
            val v = getChildAt(i)
            Log.d("BaseEasyPlayer", "v -> ${v.toString()}")
            viewList.add(v)
        }
        viewList.forEach { v ->

            when(v){
                is PlayerEngineVConfig<*> -> {
                    environmentBuilder.playerEngineFactory = v.getFactory()
                    removeView(v)
                }
                is RenderVConfig<*> -> {
                    environmentBuilder.renderFactory =  v.getFactory()
                    removeView(v)
                    // 将 renderContainer 放到 Controller 对应位置
                    realViewContainer.removeView(renderContainer)
                    realViewContainer.addView(renderContainer)
                }
                is IControllerGetter -> {
                    removeView(v)
                    addController(true, v.getController())
                }
            }
        }
    }

    fun requireEnvironment(): EasyPlayerEnvironment{
        val environment = realEnvironment.get()
        if(environment == null){
            loadEnvironment()
        }
        return realEnvironment.get()?:throw IllegalStateException("requireEnvironment failed")
    }

    fun onBackPress(): Boolean {
        if(mIsFullScreen){
            stopFullScreen()
            return true
        }
        return false
    }

    private inline fun runWithEnvironmentIfNotNull(block: EasyPlayerEnvironment.()->Unit){
        realEnvironment.get()?.let {
            it.block()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        findEnvironmentAndControllerFromChildren()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
//        PlayUtils.findActivity(context)?.let {
//            act = WeakReference(it)
//        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if(hasWindowFocus && mIsFullScreen){
            MediaHelper.setSystemBarsBehavior(requireActivity(), WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        }
    }

    private fun requireActivity(): Activity {
        return getActivity() ?: throw IllegalStateException("BaseEasyPlayer Not in Activity")
    }

    private fun getActivity(): Activity?{
        return PlayUtils.findActivity(context)
    }

    private fun requestFocusIfNeed(){
        if(!isMute() && enableAudioFocus){
            audioFocusHelper.requestFocusCompat(AudioAttributesCompat.USAGE_MEDIA, AudioAttributesCompat.CONTENT_TYPE_MOVIE)
        }
    }

    protected fun saveProgress(){
        runWithEnvironmentIfNotNull {
            mCurrentPosition = playerEngine.getCurrentPosition()
        }
        progressManager.setProgress(url, mCurrentPosition)
    }

    override fun onSaveInstanceState(): Parcelable? {
        saveProgress()
        return super.onSaveInstanceState()
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