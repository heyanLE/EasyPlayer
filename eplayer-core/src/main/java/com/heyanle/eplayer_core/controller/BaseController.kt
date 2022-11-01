package com.heyanle.eplayer_core.controller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.AttributeSet
import android.view.OrientationEventListener
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.heyanle.eplayer_core.EasyPlayerManager
import com.heyanle.eplayer_core.constant.EasyPlayStatus
import com.heyanle.eplayer_core.constant.EasyPlayerStatus
import com.heyanle.eplayer_core.player.IPlayer
import com.heyanle.eplayer_core.utils.OrientationHelper
import com.heyanle.eplayer_core.utils.PlayUtils
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 实现 Controller 的最基础功能
 * 1. Component 管理 与 事件发放
 * 2. 屏幕旋转监听管理
 * 3. 锁定状态管理
 * 4. 控制器视图显示与隐藏
 * 5. 播放器状态事件分发
 * 6. 播放引擎状态事件分发
 * 7. 播放进度监听
 * Create by heyanlin on 2022/10/25
 */
open class BaseController:
    RelativeLayout,
    IController,
    OrientationHelper.OnOrientationChangedListener
{

    var fadeOutTimeout = 4000L

    protected var activity: WeakReference<Activity>? = null

    // 组件容器和锁，key 为 IComponent ， Value 为是否添加到该 ViewGroup
    protected val components: LinkedHashMap<IComponent, Boolean> = linkedMapOf()
    protected val componentsLock = ReentrantReadWriteLock()

    protected var componentContainer: ComponentContainer? = null

    private var mIsShowing = true
    private var mIsLocked = false

    // 是否监听屏幕旋转来
    var enableOrientation = EasyPlayerManager.enableOrientation
    // 当前屏幕方向记录
    private var mOrientation = 0
    private val mOrientationHelper: OrientationHelper by lazy {
        OrientationHelper(context).also {
            it.listener = this@BaseController
        }
    }

    // 是否开始监听进度
    private var mIsStartProgress = false


    private val mHideRunnable = Runnable {
        hide()
    }

    private val mUpdateProgressRunnable = object: Runnable {
        override fun run() {
            componentContainer?.let {
                val position = it.getCurrentPosition()
                val duration = it.getDuration()
                handleProgressUpdate(duration, position)
                if(it.isPlaying()){
                    postDelayed(this, ((1000 - position % 1000) / it.getSpeed()).toLong())
                }else{
                    mIsStartProgress = false
                }
            }
        }
    }

    init {
        PlayUtils.findActivity(context)?.let {
            activity = WeakReference(it)
        }
    }

    // == BaseController 本身对外的 api，有些是代理方法

    // 绑定统一的 播放控制器 接口
    override fun attachToPlayer(playerController: IPlayer){
        val container = ComponentContainer(this, playerController)
        componentContainer = container
        runWithAllComponents {
            onAttachToContainer(container)
        }
    }

    override fun detachPlayer(iPlayer: IPlayer) {

        componentContainer?.let {
            runWithAllComponents {
                onDetachToContainer(it)
            }
        }

        componentContainer = null
    }

    open fun togglePlay(){
        componentContainer?.togglePlay()
    }

    open fun toggleFullScreen(){
        componentContainer?.toggleFullScreen()
    }

    open fun startFullScreen(){
        componentContainer?.startFullScreen()
    }

    @SuppressLint("SourceLockedOrientationActivity")
    open fun stopFullScreen(){
        val act = activity?.get()?:return
        if(act.isFinishing) return
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        componentContainer?.stopFullScreen()
    }

    // == 播放器状态和播放状态的改变事件分发 ==

    override fun dispatchPlayStateChange(playState: Int) {
        handlePlayStateChanged(playState)
    }


    override fun dispatchPlayerStateChange(playerState: Int) {
        handlePlayerStateChanged(playerState)
    }

    // 返回按钮监听，递归使用

    override fun onBackPressed(): Boolean {
        return false
    }

    // == 屏幕旋转监听逻辑 ==============================

    override fun onOrientationChanged(orientation: Int) {
        val act = activity?.get()?:return

        val lastOrientation = mOrientation
        if(orientation == OrientationEventListener.ORIENTATION_UNKNOWN){
            mOrientation = -1
            return
        }

        if(orientation > 350 || orientation < 10){
            val o: Int = act.requestedOrientation
            if (o == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && lastOrientation == 0) return
            if (mOrientation == 0) return
            //0度，用户竖直拿着手机
            mOrientation = 0
            onOrientationPortrait(act)
        } else if(orientation in 81..99) {
            val o: Int = act.requestedOrientation
            if (o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && lastOrientation == 90) return
            if (mOrientation == 90) return
            //90度，用户右侧横屏拿着手机
            mOrientation = 90
            onOrientationReverseLandscape(act)
        } else if (orientation in 261..279) {
            val o: Int = act.requestedOrientation
            //手动切换横竖屏
            if (o == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && lastOrientation == 270) return
            if (mOrientation == 270) return
            //270度，用户左侧横屏拿着手机
            mOrientation = 270
            onOrientationLandscape(act)
        }
    }

    /**
     * 竖屏
     */
    @SuppressLint("SourceLockedOrientationActivity")
    protected open fun onOrientationPortrait(activity: Activity) {
        //屏幕锁定的情况
        if (mIsLocked) return
        //没有开启设备方向监听的情况
        if (!enableOrientation) return
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        componentContainer?.stopFullScreen()
    }

    /**
     * 横屏
     */
    protected open fun onOrientationLandscape(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        if (componentContainer?.isFullScreen() == true) {
            handlePlayerStateChanged(EasyPlayerStatus.PLAYER_FULL_SCREEN)
        } else {
            componentContainer?.startFullScreen()
        }
    }

    /**
     * 反向横屏
     */
    protected open fun onOrientationReverseLandscape(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        if (componentContainer?.isFullScreen() == true) {
            handlePlayerStateChanged(EasyPlayerStatus.PLAYER_FULL_SCREEN)
        } else {
            componentContainer?.startFullScreen()
        }
    }

    // == Component 管理 =============================================

    // 注册 子 View 组件，在 onFinishInflate 调用
    private fun addComponentsByChildren(){
        for(i in 0 until childCount){
            val v = getChildAt(i)
            val cg = (v as? IComponentGetter)?:continue
            removeView(v)
            // 虽然传入 true，但是会触发安全检查，
            // 不会最终 add view，但是在 remove 时会 remove
            addComponents(true, cg.getComponent())
        }
    }

    protected inline fun runWithAllComponents(block: IComponent.()->Unit) {
        componentsLock.read {
            components.iterator().forEach {
                it.key.block()
            }
        }
    }

    override fun addComponents(isAddToViewGroup: Boolean, vararg component: IComponent) {
        componentsLock.write {
            for(c in component){
                removeComponents(c)
                components[c] = isAddToViewGroup
                componentContainer?.let {
                    c.onAttachToContainer(it)
                }
                if(isAddToViewGroup){
                    val params = c.getLayoutParam()
                    val view = c.getView()
                    if(view != null && view.parent == null){
                        if(params == null){
                            addView(view)
                        }else{
                            addView(view, params)
                        }
                    }
                }
            }
        }
    }

    override fun removeComponents(vararg component: IComponent) {
        componentsLock.write {
            for(c in component){
                removeView(c.getView())
                components.remove(c)
            }
        }
    }

    override fun removeAllComponents() {
        componentsLock.write {
            val it = components.iterator()
            while(it.hasNext()){
                val c = it.next()
                c.key.onRemove()
                removeView(c.key.getView())
                it.remove()
            }
        }
    }

    override fun removeAllComponentsWithoutAddToViewGroup() {
        componentsLock.write {
            val it = components.iterator()
            while(it.hasNext()){
                val c = it.next()
                if(!c.value){
                    it.remove()
                }
            }
        }
    }


    // == 事件 step.1 处理与分发给 Components =============================================

    protected open fun handleVisibilityChanged(isVisible: Boolean) {
        if (!mIsLocked) { //没锁住时才向ControlComponent下发此事件
            runWithAllComponents {
                onVisibleChanged(isVisible)
            }
        }
        onHandleVisibilityChanged(isVisible)
    }

    protected open fun handleLockStateChanged(locked: Boolean) {
        componentsLock.read {
            components.iterator().forEach {
                it.key.onLockStateChange(locked)
            }
        }
        onHandleLockStateChanged(locked)
    }

    protected fun handleProgressUpdate(duration: Long, position: Long){
        componentsLock.read {
            components.iterator().forEach {
                it.key.onProgressUpdate(duration, position)
            }
        }
        onHandleProgressUpdate(duration, position)
    }



    protected open fun handlePlayerStateChanged(playerState: Int) {
        componentsLock.read {
            components.iterator().forEach {
                it.key.onPlayerStateChanged(playerState)
            }
        }
        onPlayerStateChanged(playerState)
    }

    protected open fun handlePlayStateChanged(playState: Int){
        componentsLock.read {
            components.iterator().forEach {
                it.key.onPlayStateChanged(playState)
            }
        }
        onPlayStateChanged(playState)
    }

    // == 事件 step.2 分发给子类，子类可重写 =============================================

    protected open fun onHandleVisibilityChanged(isVisible: Boolean){}

    protected open fun onHandleLockStateChanged(isLocked: Boolean){}

    protected open fun onHandleProgressUpdate(duration: Long, position: Long) {}

    protected open fun onPlayerStateChanged(playerState: Int) {
        when (playerState) {
            EasyPlayerStatus.PLAYER_NORMAL -> {
                if(enableOrientation){
                    mOrientationHelper.enable()
                }else{
                    mOrientationHelper.disable()
                }
            }
            EasyPlayerStatus.PLAYER_FULL_SCREEN -> {
                mOrientationHelper.enable()
            }
            EasyPlayerStatus.PLAYER_TINY_SCREEN -> {
                mOrientationHelper.disable()
            }
        }
    }

    protected open fun onPlayStateChanged(playState: Int){
        when (playState) {
            EasyPlayStatus.STATE_IDLE -> {
                mOrientationHelper.disable()
                mOrientation = 0
                mIsLocked = false
                mIsShowing = false
                //由于游离组件是独立于控制器存在的，
                //所以在播放器release的时候需要移除
                removeAllComponentsWithoutAddToViewGroup()
            }
            EasyPlayStatus.STATE_PLAYBACK_COMPLETED -> {
                mIsLocked = false
                mIsShowing = false
            }
            EasyPlayStatus.STATE_ERROR -> mIsShowing = false
        }
    }

    // == IController override ===========

    override fun getViewContainer(): ViewGroup {
        return this
    }

    override fun startFadeOut() {
        removeCallbacks(mHideRunnable)
        postDelayed(mHideRunnable, fadeOutTimeout)
    }

    override fun stopFadeOut() {
        removeCallbacks(mHideRunnable)
    }

    override fun isShowing(): Boolean {
        return mIsShowing
    }

    override fun setLocked(locked: Boolean) {
        mIsLocked = locked
        handleLockStateChanged(locked)
    }

    override fun isLocked(): Boolean {
        return mIsLocked
    }

    override fun startProgressUpdate() {
        mIsStartProgress = true
        removeCallbacks(mUpdateProgressRunnable)
        post(mUpdateProgressRunnable)
    }

    override fun stopProgressUpdate() {
        mIsStartProgress = false
        removeCallbacks(mUpdateProgressRunnable)
    }

    override fun show() {
        if(!mIsShowing){
            startFadeOut()
            handleVisibilityChanged(true)
            mIsShowing = true
        }
    }

    override fun hide() {
        if(mIsShowing){
            stopFadeOut()
            handleVisibilityChanged(false)
            mIsShowing = false
        }
    }

    // == View override ===========

    override fun onFinishInflate() {
        super.onFinishInflate()
        addComponentsByChildren()
    }

    // 全屏时焦点处理
    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (componentContainer?.isPlaying() == true
            && (enableOrientation || componentContainer?.isFullScreen() == true)
        ) {
            if (hasWindowFocus) {
                postDelayed({ mOrientationHelper.enable() }, 800)
            } else {
                mOrientationHelper.disable()
            }
        }
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