package com.heyanle.lib.render

import android.content.Context
import com.heyanle.eplayer_core.render.IRender

/**
 * Created by HeYanLe on 2022/10/23 15:07.
 * https://github.com/heyanLE
 */
interface IRenderFactory {

    fun createRender(context: Context): IRender

}