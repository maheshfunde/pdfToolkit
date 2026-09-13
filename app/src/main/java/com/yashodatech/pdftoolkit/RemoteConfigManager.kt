package com.yashodatech.pdftoolkit

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await

object RemoteConfigManager {

    private const val SHOW_ADS_KEY = "show_ads"

    private val remoteConfig: FirebaseRemoteConfig? by lazy {
        try {
            FirebaseRemoteConfig.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun initialize() {
        try {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600 // 1 hour
            }
            remoteConfig?.setConfigSettingsAsync(configSettings)
            remoteConfig?.setDefaultsAsync(mapOf(SHOW_ADS_KEY to false))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig?.fetchAndActivate()?.await() ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false // Fetch failed, defaults will be used
        }
    }

    fun shouldShowAds(): Boolean {
        return try {
            remoteConfig?.getBoolean(SHOW_ADS_KEY) ?: false
        } catch (e: Exception) {
            false
        }
    }
}