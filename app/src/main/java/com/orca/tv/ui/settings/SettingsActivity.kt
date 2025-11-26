package com.orca.tv.ui.settings

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.orca.tv.R

/**
 * 设置界面 Activity
 */
class SettingsActivity : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_fragment_container, SettingsFragment())
                .commit()
        }
    }
}
