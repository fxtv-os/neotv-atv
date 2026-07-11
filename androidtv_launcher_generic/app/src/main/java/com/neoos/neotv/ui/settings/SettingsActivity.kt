package com.neoos.neotv.ui.settings

import android.os.Bundle
import androidx.fragment.app.commit
import com.neoos.neotv.R
import com.neoos.neotv.util.BaseActivity

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.settings_container, SettingsFragment())
            }
        }
    }
}
