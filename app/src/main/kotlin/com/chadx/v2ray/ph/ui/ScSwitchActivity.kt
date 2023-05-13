package com.chadx.v2ray.ph.ui

import com.chadx.v2ray.ph.R
import com.chadx.v2ray.ph.util.Utils
import android.os.Bundle
import com.chadx.v2ray.ph.service.V2RayServiceManager

class ScSwitchActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        moveTaskToBack(true)

        setContentView(R.layout.activity_none)

        if (V2RayServiceManager.v2rayPoint.isRunning) {
            Utils.stopVService(this)
        } else {
            Utils.startVServiceFromToggle(this)
        }
        finish()
    }
}
