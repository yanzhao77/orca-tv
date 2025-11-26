package com.orca.tv.ui.settings

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.preference.LeanbackEditTextPreferenceDialogFragmentCompat
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import com.orca.tv.R

/**
 * 设置 Fragment
 * 使用 Leanback Preference 实现 TV 友好的设置界面
 */
class SettingsFragment : LeanbackPreferenceFragmentCompat() {
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        // API 地址设置
        val apiUrlPreference = findPreference<EditTextPreference>("api_url")
        apiUrlPreference?.setOnPreferenceChangeListener { _, newValue ->
            val url = newValue as String
            if (url.isNotEmpty() && !url.startsWith("http")) {
                Toast.makeText(requireContext(), "请输入有效的 HTTP 地址", Toast.LENGTH_SHORT).show()
                false
            } else {
                Toast.makeText(requireContext(), "API 地址已保存", Toast.LENGTH_SHORT).show()
                true
            }
        }
        
        // 显示当前 API 地址
        updateApiUrlSummary()
    }
    
    private fun updateApiUrlSummary() {
        val apiUrlPreference = findPreference<EditTextPreference>("api_url")
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val currentUrl = prefs.getString("api_url", "未配置")
        apiUrlPreference?.summary = currentUrl
    }
    
    override fun onPreferenceDisplayDialog(
        caller: androidx.preference.PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        if (pref is EditTextPreference) {
            val fragment = LeanbackEditTextPreferenceDialogFragmentCompat.newInstance(pref.key)
            fragment.setTargetFragment(caller, 0)
            fragment.show(caller.parentFragmentManager, "androidx.preference.PreferenceFragment.DIALOG")
            return true
        }
        return false
    }
}
