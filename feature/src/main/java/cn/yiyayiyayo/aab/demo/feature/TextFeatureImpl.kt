package cn.yiyayiyayo.aab.demo.feature

import android.content.Context
import cn.yiyayiyayo.aab.demo.TextFeature

class TextFeatureImpl(private val context: Context) : TextFeature {
    override fun getText(): CharSequence =
        context.getString(R.string.test_text)

    companion object Provider : TextFeature.Provider {
        override fun get(dependencies: TextFeature.Dependencies): TextFeature =
            TextFeatureImpl(dependencies.getContext())
    }
}