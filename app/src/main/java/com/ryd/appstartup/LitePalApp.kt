package com.ryd.appstartup

import android.app.Application
import android.content.Context
import org.litepal.LitePal

/**
 * 常规方式 在应用启动时初始化需要Context的类库，但如果（用于讲解App Startup使用）
 */
public class LitePalApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LitePal.initialize(this)
    }
}