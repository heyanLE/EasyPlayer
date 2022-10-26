package com.heyanle.eplayer_core.player

import androidx.collection.LruCache

/**
 * Create by heyanlin on 2022/10/26
 */
interface ProgressManager {

    companion object {
        fun ofDefault(): ProgressManager {
            return LruMemoryProgressManager()
        }
    }

    fun getProgress(key: String): Long

    fun setProgress(key: String, progress: Long)

}

class LruMemoryProgressManager: ProgressManager {

    private val lruCache = LruCache<String, Long>(100)

    override fun getProgress(key: String): Long {
        return lruCache[key]?:0L
    }

    override fun setProgress(key: String, progress: Long) {
        lruCache.put(key, progress)
    }
}