package com.heyanle.eplayer_core.controller

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import com.heyanle.eplayer_core.player.IPlayer

/**
 * 将 IController 和 IPlayer 的接口合并，供 Component 调用
 * Create by heyanlin on 2022/10/25
 */
class ComponentContainer(
    controller: IController,
    player: IPlayer,
) : IController by controller, IPlayer by player{

    fun togglePlay(){
        if(isPlaying()){
            pause()
        }else{
            start()
        }
    }

    fun toggleFullScreen(){
        // 这里调用 IPlayer 的 api
        if(isFullScreen()){
            stopFullScreen()
        }else{
            startFullScreen()
        }
    }

    fun toggleLockState(){
        setLocked(!isLocked())
    }

    fun toggleShowState(){
        if(!isShowing()){
            hide()
        }else{
            show()
        }
    }

}