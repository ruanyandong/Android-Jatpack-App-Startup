package com.ryd.appstartup

import android.content.Context
import androidx.startup.Initializer
import org.litepal.LitePal

/**
 * Kotlin写法
 */
class KtLitePalInitializer : Initializer<Unit>{
    override fun create(context: Context) {
        LitePal.initialize(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

}