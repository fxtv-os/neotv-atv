package com.neoos.neotv.util

import android.content.Context
import androidx.fragment.app.FragmentActivity

/** All activities extend this so language selection applies app-wide. */
open class BaseActivity : FragmentActivity() {

    private var attachedLanguage: String? = null

    override fun attachBaseContext(newBase: Context) {
        attachedLanguage = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onResume() {
        super.onResume()
        // If the language was changed on the Settings screen while this
        // activity was in the back stack, refresh it now that it's visible again.
        if (attachedLanguage != null && attachedLanguage != LocaleHelper.getLanguage(this)) {
            recreate()
        }
    }
}
