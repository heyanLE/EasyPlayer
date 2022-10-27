package com.heyanle.eplayer_core.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo

/**
 * Create by heyanlin on 2022/10/27
 */
object ActivityScreenHelper {

    // 将 activity 锁竖屏
    @SuppressLint("SourceLockedOrientationActivity")
    fun activityScreenOrientationPortrait(activity: Activity){
        if(activity.isFinishing)
            return

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    // 将 activity 锁横屏
    fun activityScreenOrientationLandscape(activity: Activity){
        if(activity.isFinishing)
            return

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    // 将 activity 屏幕设置为未指定（自适应）
    fun activityScreenOrientationUnspecified(activity: Activity){
        if(activity.isFinishing)
            return

        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

}