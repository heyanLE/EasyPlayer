package com.heyanle.eplayer_core.easy_player

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.FrameLayout
import com.heyanle.eplayer_core.EasyPlayerManager
import com.heyanle.eplayer_core.constant.EasyPlayStatus
import com.heyanle.eplayer_core.constant.EasyPlayerStatus
import com.heyanle.eplayer_core.constant.OtherPlayerEvent
import com.heyanle.eplayer_core.controller.IController
import com.heyanle.eplayer_core.controller.IControllerGetter
import com.heyanle.eplayer_core.player.IPlayer
import com.heyanle.eplayer_core.player.IPlayerEngine
import com.heyanle.eplayer_core.player.IPlayerEngineFactory
import com.heyanle.eplayer_core.render.IRenderFactory
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
    IPlayerEngine.EventListener
{

    private val renderContainer = FrameLayout(context)

    // 环境
    private val realEnvironment = AtomicReference<EasyPlayerEnvironment?>(null)

    // 环境创建器
    private var environmentBuilder = EasyPlayerEnvironment.Builder()

    protected val controllers: LinkedHashSet<IController> = LinkedHashSet()
    protected val controllerLock = ReentrantReadWriteLock()

    protected var url: String = ""
    protected var headers: Map<String, String> = hashMapOf()

    protected var fd: AssetFileDescriptor? = null

    private var mCurrentPlayerState = EasyPlayerStatus.PLAYER_NORMAL
    private var mCurrentPlayState = EasyPlayStatus.STATE_IDLE

    private var mCurrentPosition: Long = 0L
    var progressManager = EasyPlayerManager.progressManager

    var isLooping: Boolean = false
    private var mIsMute: Boolean = false
    private var mCurrentScaleType: Int = EasyPlayerManager.screenScaleType

    // == Controller 管理和 事件分发 ============

    fun addController(isAddToParent: Boolean = true, vararg controller: IController){
        controllerLock.write {
            for(c in controller){
                controllers.add(c)
                c.attachToPlayer(this)
                if(isAddToParent){
                    addView(c.getViewContainer())
                }
            }
        }
    }

    fun removeController(vararg controller: IController) {
        controllerLock.write {
            for(c in controller){
                controllers.remove(c)
                c.detachPlayer(this)
                removeView(c.getViewContainer())
            }
        }
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
        if (isInIdleState()
            || isInStartAbortState()
        ) {
            startFirst()
        } else if (isInPlaybackState()) {
            startInPlaybackState()
        }
    }

    override fun pause() {
        runWithEnvironmentIfNotNull {
            if(isInPlaybackState() && playerEngine.isPlaying()){
                playerEngine.pause()
                dispatchPlayStateChange(EasyPlayStatus.STATE_PAUSED)
                renderContainer.keepScreenOn = false
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
                mCurrentPosition = playerEngine.getCurrentPosition()
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

    override fun startFullScreen() {

    }

    override fun stopFullScreen() {
        TODO("Not yet implemented")
    }

    override fun isFullScreen(): Boolean {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun setPlayerRotation(rotation: Float) {
        runWithEnvironmentIfNotNull {
            render.setVideoRotation(rotation.toInt())
        }
    }

    override fun startTinyScreen(): Boolean {

    }

    override fun stopTinyScreen() {

    }

    override fun isTinyScreen(): Boolean {

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
        if(mCurrentPosition > 0){
            seekTo(mCurrentPosition)
        }
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {

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

    // == PlayerView 自带相关逻辑 ===============


    protected open fun startFirst(): Boolean{
        if(url.isNotEmpty() ){
            mCurrentPosition = progressManager.getProgress(url)
        }
        startPrepare()
        return true
    }

    protected open fun startInPlaybackState(){
        val environment = requireEnvironment()
        dispatchPlayStateChange(EasyPlayStatus.STATE_PLAYING)
        renderContainer.keepScreenOn = true
        environment.playerEngine.start()

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
            renderContainer.addView(new.render.getView())
        }
    }

    private fun findEnvironmentAndControllerFromChildren(){
        for(i in 0 until childCount){
            when(val v = getChildAt(i)){
                is IPlayerEngineFactory -> {
                    environmentBuilder.playerEngineFactory = v
                    removeView(v)
                }
                is IRenderFactory -> {
                    environmentBuilder.renderFactory =  v
                    removeView(v)
                }
                is IControllerGetter -> {
                    addController(false, v.getController())
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

    private inline fun runWithEnvironmentIfNotNull(block: EasyPlayerEnvironment.()->Unit){
        realEnvironment.get()?.let {
            it.block()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        findEnvironmentAndControllerFromChildren()
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