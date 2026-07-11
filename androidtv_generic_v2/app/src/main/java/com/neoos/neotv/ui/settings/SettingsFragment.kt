package com.neoos.neotv.ui.settings

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.neoos.neotv.R
import com.neoos.neotv.util.LocaleHelper

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<ListPreference>("pref_language")?.setOnPreferenceChangeListener { _, newValue ->
            LocaleHelper.setLanguage(requireContext(), newValue as String)
            // Recreate so the settings screen itself updates immediately.
            requireActivity().recreate()
            true
        }
    }
}
