package com.heyanle.easyplayer.utils

/**
 * Create by heyanlin on 2022/10/28
 */
object TimeUtils {

    fun toString(time: Long): String{
        return "${(time/60).toInt()}:${(time%60).toInt()}"
    }

}