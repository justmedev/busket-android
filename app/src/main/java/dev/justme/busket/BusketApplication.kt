package dev.justme.busket

import android.app.Application
import com.google.android.material.color.DynamicColors

class BusketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
