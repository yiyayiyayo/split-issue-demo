package cn.yiyayiyayo.aab.demo

import android.content.Context

interface TextFeature {
    fun getText(): CharSequence

    interface Provider {
        fun get(dependencies: Dependencies): TextFeature
    }

    /**
     * Dependencies from the main app module that are required by the StorageFeature.
     */
    interface Dependencies {
        fun getContext(): Context
    }
}