package com.freellm.superpro

import android.app.Application
import com.freellm.superpro.data.local.AppDatabase

class SuperProApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppDatabase.getInstance(this)
    }
}
