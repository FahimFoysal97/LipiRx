package com.snehonir.lipirx

import android.app.Application
import com.snehonir.lipirx.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext

class LipiRxApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        initKoin {
            androidContext(this@LipiRxApplication)
        }
    }
}
