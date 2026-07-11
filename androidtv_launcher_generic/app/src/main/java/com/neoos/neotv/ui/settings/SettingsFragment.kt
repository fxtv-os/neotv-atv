package com.neoos.neotv.ui.settings

import android.os.Bundle
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import com.neoos.neotv.R
import com.neoos.neotv.util.LocaleHelper

class SettingsFragment : LeanbackPreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<androidx.preference.ListPreference>("pref_language")?.setOnPreferenceChangeListener { _, newValue ->
            LocaleHelper.setLanguage(requireContext(), newValue as String)
            // Recreate so the settings screen itself updates immediately.
            requireActivity().recreate()
            true
        }
    }
}
