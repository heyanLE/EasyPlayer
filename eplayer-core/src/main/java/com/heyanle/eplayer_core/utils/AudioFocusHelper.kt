package com.heyanle.eplayer_core.utils

import android.content.Context
import android.media.AudioManager
import androidx.media.AudioAttributesCompat
import androidx.media.AudioFocusRequestCompat
import androidx.media.AudioManagerCompat
import java.lang.ref.WeakReference

/**
 * Create by heyanlin on 2022/11/1
 */
class AudioFocusHelper(context: Context): AudioManager.OnAudioFocusChangeListener{

    interface OnAudioFocusListener {

        // 获取焦点
        fun onAudioFocusChange(focusChange: Int)
    }

    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequestCompat? = null

    private var currentAudioFocus = -1

    private var listener: WeakReference<OnAudioFocusListener>? = null
    fun setListener(listener: OnAudioFocusListener){
        this.listener = WeakReference(listener)
    }


    fun requestFocusCompat(
        usage: Int,
        content: Int,
        requestFocus: Int = AudioManagerCompat.AUDIOFOCUS_GAIN,
    ): Int {
        if(request != null){
            abandonFocus()
        }
        val req = AudioFocusRequestCompat.Builder(requestFocus).run {
            setAudioAttributes(AudioAttributesCompat.Builder().run {
                setContentType(content)
                setUsage(usage)
                setWillPauseWhenDucked(true)
                build()
            })
            setOnAudioFocusChangeListener(this@AudioFocusHelper)
            build()
        }
        request = req
        return AudioManagerCompat.requestAudioFocus(audioManager, req)
    }


    fun abandonFocus() {
        request?.let {
            AudioManagerCompat.abandonAudioFocusRequest(audioManager, it)
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        // 防抖
        if(currentAudioFocus == focusChange){
            return
        }
        currentAudioFocus = focusChange
        listener?.get()?.onAudioFocusChange(focusChange)
    }
}

