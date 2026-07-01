package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

/**
 * 管理应用内语言切换，持久化到 SharedPreferences。
 * 使用 createConfigurationContext + attachBaseContext 方案，
 * 兼容所有 Android 版本，切换后通过 Activity.recreate() 即时生效。
 */
object LanguageManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_LANGUAGE = "language"
    const val LANG_ZH = "zh"
    const val LANG_EN = "en"

    fun getSavedLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANG_ZH) ?: LANG_ZH
    }

    fun setLanguage(context: Context, lang: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
    }

    /**
     * 根据已保存的语言创建带 locale 的 Context。
     * Activity 在 attachBaseContext 中调用此方法。
     */
    fun apply(savedLang: String, base: Context): Context {
        val locale = when (savedLang) {
            LANG_EN -> Locale.ENGLISH
            else -> Locale.SIMPLIFIED_CHINESE
        }
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
