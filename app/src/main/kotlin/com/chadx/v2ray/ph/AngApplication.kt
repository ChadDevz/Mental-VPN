package com.chadx.v2ray.ph

import android.content.Intent
import android.widget.Toast
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import com.google.android.gms.ads.MobileAds
import com.tencent.mmkv.MMKV
import com.chadx.v2ray.ph.OpenAds.AppOpenManager
import java.io.PrintWriter
import java.io.StringWriter


class AngApplication : MultiDexApplication() {
    companion object {
        const val PREF_LAST_VERSION = "pref_last_version"
    }

    private lateinit var appOpenManager: AppOpenManager

    var curIndex = -1 //Current proxy that is opened. (Used to implement restart feature)
    var firstRun = false
        private set

    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this) {}
        appOpenManager = AppOpenManager(this)

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable -> // get the stack trace
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)

            // send the  String into Another Activity
            val i = Intent(applicationContext, XCronoksReportCrash2::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            i.putExtra("XReport", sw.toString())
            startActivity(i)
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
        }

//        LeakCanary.install(this)

        val defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
       firstRun = defaultSharedPreferences.getInt(PREF_LAST_VERSION, 0) != BuildConfig.VERSION_CODE
        if (firstRun)
            defaultSharedPreferences.edit().putInt(PREF_LAST_VERSION, BuildConfig.VERSION_CODE).apply()

        //Logger.init().logLevel(if (BuildConfig.DEBUG) LogLevel.FULL else LogLevel.NONE)
        MMKV.initialize(this)
    }
}
