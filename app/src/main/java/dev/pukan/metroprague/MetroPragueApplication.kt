package dev.pukan.metroprague

import android.app.Application
import dev.pukan.metroprague.di.AppContainer
import dev.pukan.metroprague.di.DefaultAppContainer

class MetroPragueApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer()
    }
}
