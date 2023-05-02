package dev.justme.busket

import android.app.Application
import android.content.res.Resources
import com.google.android.material.color.DynamicColors

class BusketApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        appResources = resources
    }

    companion object {
        var appResources: Resources? = null
            private set
    }
}
