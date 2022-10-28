package com.heyanle.easyplayer

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.heyanle.easyplayer.databinding.ActivityPlayerBinding

/**
 * Create by heyanlin on 2022/10/28
 */
class TestActivity: AppCompatActivity() {

    val binding :ActivityPlayerBinding by lazy {
        ActivityPlayerBinding.inflate(LayoutInflater.from(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.playerView.setDataSource("https://yun.ssdm.cc/SBDM/BocchitheRock03.m3u8")
        //.playerView.start()
    }

    override fun onResume() {
        super.onResume()
        binding.playerView.start()
    }

}