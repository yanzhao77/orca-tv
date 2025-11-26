package com.orca.tv.ui.settings

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.orca.tv.R

/**
 * 设置 Fragment
 * 使用 Leanback Preference 实现 TV 友好的设置界面
 */
class SettingsFragment : LeanbackSettingsFragmentCompat() {
    
    override fun onPreferenceStartInitialScreen() {
        startPreferenceFragment(buildPreferenceFragment(R.xml.preferences, null))
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        return false
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        pref: PreferenceScreen
    ): Boolean {
        val fragment = buildPreferenceFragment(R.xml.preferences, pref.key)
        startPreferenceFragment(fragment)
        return true
    }

    private fun buildPreferenceFragment(preferenceResId: Int, root: String?): PreferenceFragmentCompat {
        return PrefsFragment.newInstance(preferenceResId, root)
    }

    /**
     * 内部 Preference Fragment
     */
    class PrefsFragment : LeanbackPreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            val preferenceResId = arguments?.getInt(ARG_PREFERENCE_RESOURCE_ID)
                ?: throw IllegalArgumentException("Preference resource ID is required")
            
            setPreferencesFromResource(preferenceResId, rootKey)
            
            // API 地址设置
            val apiUrlPreference = findPreference<EditTextPreference>("api_url")
            apiUrlPreference?.setOnPreferenceChangeListener { _, newValue ->
                val url = newValue as String
                if (url.isNotEmpty() && !url.startsWith("http")) {
                    Toast.makeText(context, "请输入有效的 HTTP 地址", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Toast.makeText(context, "API 地址已保存", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            
            // 显示当前 API 地址
            updateApiUrlSummary()
        }
        
        private fun updateApiUrlSummary() {
            val apiUrlPreference = findPreference<EditTextPreference>("api_url")
            val prefs = context?.getSharedPreferences("settings", Context.MODE_PRIVATE)
            val currentUrl = prefs?.getString("api_url", "未配置") ?: "未配置"
            apiUrlPreference?.summary = currentUrl
        }

        companion object {
            private const val ARG_PREFERENCE_RESOURCE_ID = "preference_resource_id"
            private const val ARG_PREFERENCE_ROOT = "preference_root"

            fun newInstance(preferenceResId: Int, root: String?): PrefsFragment {
                val fragment = PrefsFragment()
                val args = Bundle().apply {
                    putInt(ARG_PREFERENCE_RESOURCE_ID, preferenceResId)
                    putString(ARG_PREFERENCE_ROOT, root)
                }
                fragment.arguments = args
                return fragment
            }
        }
    }
}
