package com.objectmeasure.ar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class com Hilt configurado
 * DIA 3: Dependency Injection habilitado para todo o app
 */
@HiltAndroidApp
class ARObjectMeasureApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Futuro: configurações de inicialização do app
    }
}