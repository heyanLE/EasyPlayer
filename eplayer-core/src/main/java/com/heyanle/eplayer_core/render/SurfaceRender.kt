package com.heyanle.eplayer_core.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.util.AttributeSet
import android.view.*
import com.heyanle.eplayer_core.player.IPlayerEngine
import com.heyanle.eplayer_core.render.IRender
import com.heyanle.eplayer_core.utils.MeasureHelper

/**
 * Created by HeYanLe on 2022/10/23 15:39.
 * https://github.com/heyanLE
 */
class SurfaceRender: SurfaceView, IRender, SurfaceHolder.Callback {

    private val measureHelper = MeasureHelper()
    private var easyPlayer: IPlayerEngine? = null

    init {
        val holder = holder
        holder.addCallback(this)
        holder.setFormat(PixelFormat.RGBA_8888)
    }

    override fun detachPlayerEngine(player: IPlayerEngine) {
        this.easyPlayer = null
    }

    override fun attachToPlayerEngine(player: IPlayerEngine) {
        this.easyPlayer = player
    }

    override fun setVideoSize(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            measureHelper.setVideoSize(width, height)
            requestLayout()
        }
    }

    override fun setVideoRotation(degree: Int) {
        measureHelper.setVideoRotation(degree)
        requestLayout()
    }

    override fun setScaleType(scaleType: Int) {
        measureHelper.setScreenScale(scaleType)
    }

    override fun getView(): View {
        return this
    }

    override fun beforeAddToWindow(view: View, parent: ViewGroup) {

    }

    override fun screenShot(): Bitmap? {
        return null
    }

    override fun release() {

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredSize: IntArray = measureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredSize[0], measuredSize[1])
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        easyPlayer?.setSurfaceHolder(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // easyPlayer?.setSurfaceHolder(holder)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
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