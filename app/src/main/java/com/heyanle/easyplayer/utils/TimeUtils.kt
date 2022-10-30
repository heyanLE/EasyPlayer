package com.heyanle.easyplayer.utils

/**
 * Create by heyanlin on 2022/10/28
 */
object TimeUtils {

    fun toString(time: Long): String{
        val min = (time/60).toInt()
        val se = (time%60).toInt()

        return "${if(min>=10)min else "0$min"}:${if(se>=10)se else "0$se"}"
    }

}