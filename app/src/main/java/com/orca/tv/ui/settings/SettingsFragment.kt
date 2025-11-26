package com.orca.tv.ui.settings

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.orca.tv.R

/**
 * 设置 Fragment
 * 使用基础 PreferenceFragmentCompat 实现设置界面
 */
class SettingsFragment : PreferenceFragmentCompat() {
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
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
}
