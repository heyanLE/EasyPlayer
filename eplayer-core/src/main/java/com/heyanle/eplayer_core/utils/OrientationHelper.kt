package com.heyanle.eplayer_core.utils

import android.content.Context
import android.media.AudioFocusRequest
import android.view.OrientationEventListener

/**
 * Create by heyanlin on 2022/11/1
 */
class OrientationHelper(context: Context): OrientationEventListener(context) {

    companion object {
        // 防抖冷却处理
        private const val ORIENTATION_CHANGE_CD = 300
    }

    private var mLastTime: Long = System.currentTimeMillis()

    interface OnOrientationChangedListener {
        fun onOrientationChanged(orientation: Int)
    }

    var listener: OnOrientationChangedListener? = null

    override fun onOrientationChanged(orientation: Int) {

        val currentTime = System.currentTimeMillis()
        if (currentTime - mLastTime < ORIENTATION_CHANGE_CD) return  // 防抖处理

        listener?.onOrientationChanged(orientation)
        mLastTime = currentTime

    }
}