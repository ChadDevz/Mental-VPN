package com.chadx.v2ray.ph.ui

import android.graphics.Color
import android.os.Bundle
import com.chadx.v2ray.ph.R
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import com.chadx.v2ray.ph.AppConfig
import com.chadx.v2ray.ph.databinding.ActivityRoutingSettingsBinding

class RoutingSettingsActivity : BaseActivity() {
    private lateinit var binding: ActivityRoutingSettingsBinding

    private val titles: Array<out String> by lazy {
        resources.getStringArray(R.array.routing_tag)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRoutingSettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        title = getString(R.string.routing_settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fragments = ArrayList<Fragment>()
        fragments.add(RoutingSettingsFragment().newInstance(AppConfig.PREF_V2RAY_ROUTING_AGENT))
        fragments.add(RoutingSettingsFragment().newInstance(AppConfig.PREF_V2RAY_ROUTING_DIRECT))
        fragments.add(RoutingSettingsFragment().newInstance(AppConfig.PREF_V2RAY_ROUTING_BLOCKED))

        val adapter = FragmentAdapter(this, fragments)
        binding.viewpager.adapter = adapter
        binding.tablayout.setTabTextColors(Color.BLACK, Color.RED)
        TabLayoutMediator(binding.tablayout, binding.viewpager) { tab, position ->
            tab.text = titles[position]
        }.attach()
    }
}
