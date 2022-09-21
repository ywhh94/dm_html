package com.ywh.dm.html

import android.app.Application

/**
 * 由 Harreke 创建于 2017/9/27.
 */
object MetricsUtil {
    var Density = 1F
    var ScaledDensity = 1F
    var WidthPixels = 1
    var HeightPixels = 1
    var TouchSlop = 0F
    var ScreenRatio = 1F

    fun init(application: Application) {
        val displayMetrics = application.resources.displayMetrics
        Density = displayMetrics.density
        ScaledDensity = displayMetrics.scaledDensity
        WidthPixels = displayMetrics.widthPixels
        HeightPixels = displayMetrics.heightPixels
        ScreenRatio = HeightPixels.toFloat() / WidthPixels
        TouchSlop = 8F * Density
    }

    fun getDP(pixel: Int): Float = pixel / Density

    fun getPixel(dp: Float): Int = (dp * Density + 0.5F).toInt()

    fun px2sp(pxValue: Float): Int {
        return (pxValue / ScaledDensity + 0.5f).toInt()
    }

    fun sp2px(spValue: Float): Int {
        return (spValue * ScaledDensity + 0.5f).toInt()
    }
}