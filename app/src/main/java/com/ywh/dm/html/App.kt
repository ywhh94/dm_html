package com.ywh.dm.html

import android.app.Application

/**
 * Create by yangwenhao on 2022/9/21
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        MetricsUtil.init(this)
    }
}