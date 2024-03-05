package com.example.runningapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree

@HiltAndroidApp
class BaseApplication : Application(){

    override fun onCreate() {
        super.onCreate()
        Timber.plant(DebugTree())
//        MapsInitializer.initialize(applicationContext, Renderer.LEGACY, this)

    }
    //,OnMapsSdkInitializedCallback
//    override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
//        when (renderer) {
//            Renderer.LATEST -> Log.d("MapsDemo", "The latest version of the renderer is used.")
//            Renderer.LEGACY -> Log.d("MapsDemo", "The legacy version of the renderer is used.")
//        }
//    }
}
