package com.heyanle.eplayer_core.utils

import android.util.Log

/**
 * Create by heyanlin on 2022/11/7
 */
class FigureCounter {

    var max = 1.0f
    var min = 0.0f

    var outMax = 100
    var outMin = 0
    

    var deltaSum = 0.0f



    fun add(delta: Float): Int{
        Log.d("FigureCounter", "in($min,$max) out($outMin,$outMax) deltaSum $deltaSum delta $delta")
        deltaSum += delta

        val curDelta = ((deltaSum/(max-min))*(outMax - outMin) + 0.5f).toInt()
        return if(curDelta == 0){
            0
        }else{
            deltaSum = 0f
            curDelta
        }

    }



}