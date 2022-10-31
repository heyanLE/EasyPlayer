package com.heyanle.eplayer_core.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import com.heyanle.eplayer_core.player.IPlayerEngine
import com.heyanle.eplayer_core.utils.MeasureHelper

/**
 * Create by heyanlin on 2022/10/31
 */
class TextureViewRender: TextureView, IRender, TextureView.SurfaceTextureListener {

    private val measureHelper = MeasureHelper()
    private var easyPlayer: IPlayerEngine? = null
    private var mSurface: Surface? = null
    private var mSurfaceTexture: SurfaceTexture? = null


    // == Override IRender ==============================

    override fun detachPlayerEngine(player: IPlayerEngine) {
        easyPlayer = null
    }

    override fun attachToPlayerEngine(player: IPlayerEngine) {
        easyPlayer = player
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
        return bitmap
    }

    override fun release() {
        mSurface?.release()
        mSurfaceTexture?.release()
    }

    // == Override TextureView.SurfaceTextureListener ===============

    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
        if (mSurfaceTexture != null) {
            mSurfaceTexture?.let(::setSurfaceTexture)
        } else {
            mSurfaceTexture = surfaceTexture
            val sur = Surface(surfaceTexture)
            mSurface = sur
            easyPlayer?.setSurface(sur)
        }
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

    }

    // == Override View =============

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val measuredSize: IntArray = measureHelper.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(measuredSize[0], measuredSize[1])
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