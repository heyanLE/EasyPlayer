package com.heyanle.easyplayer

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.heyanle.easyplayer.databinding.ActivityPlayerBinding
import com.heyanle.eplayer_core.utils.MediaHelper

/**
 * Create by heyanlin on 2022/10/28
 */
class TestActivity: AppCompatActivity() {

    private val binding :ActivityPlayerBinding by lazy {
        ActivityPlayerBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.playerView.setDataSource("https://yun.ssdm.cc/SBDM/BocchitheRock03.m3u8")
        binding.playerView.start()
    //.playerView.start()
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.post {
            binding.playerView.startFullScreen()
        }
//        binding.playerView.start()
    }

    override fun onBackPressed() {
        if(binding.playerView.onBackPress()){
            return
        }
        super.onBackPressed()
    }

}