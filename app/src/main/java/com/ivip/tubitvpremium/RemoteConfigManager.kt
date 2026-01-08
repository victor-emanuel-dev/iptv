package com.ivip.tvexpressmedia

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"

    private var remoteConfig: FirebaseRemoteConfig? = null

    fun initialize() {
        if (remoteConfig != null) {
            Log.d(TAG, "Remote Config já foi inicializado")
            return
        }

        try {
            Log.d(TAG, "🔧 Inicializando Remote Config...")

            // ========== NÃO INICIALIZAR FIREBASE AQUI ========== //
            // Firebase já foi inicializado no MyApplication

            remoteConfig = Firebase.remoteConfig

            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 0  // Fetch imediato para desenvolvimento
            }

            remoteConfig?.setConfigSettingsAsync(configSettings)

            val defaults = hashMapOf<String, Any>(
                "enabled" to true,
                "api_url" to "",
                "show_ads" to true,
                "maintenance_mode" to false,
                "title" to "🎬 ACESSO PREMIUM",
                "subtitle" to "Desbloqueie todos os FILMES, SÉRIES e TV AO VIVO",
                "wp1" to "5511999999999",
                "wp2" to "5527995768269",
                "wp3" to "5527997526988",
                "tg" to "Goldvipinforpodutos",
                "debug_info" to ""
            )

            Log.d(TAG, "📋 Setando defaults...")
            remoteConfig?.setDefaultsAsync(defaults)?.addOnCompleteListener {
                Log.d(TAG, "✅ Defaults configurados")
            }

            Log.d(TAG, "🔄 Fazendo fetch do Firebase...")
            remoteConfig?.fetchAndActivate()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val updated = task.result
                        Log.d(TAG, "========================================")
                        Log.d(TAG, "✅✅✅ FIREBASE SINCRONIZADO! ✅✅✅")
                        Log.d(TAG, "Valores atualizados: $updated")
                        Log.d(TAG, "========================================")
                        logAllValues()
                    } else {
                        Log.e(TAG, "❌ ERRO ao fazer fetch do Firebase!")
                        Log.e(TAG, "Usando valores DEFAULT", task.exception)
                        logAllValues()
                    }
                }

            Log.d(TAG, "✅ Remote Config inicializado")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao inicializar", e)
            e.printStackTrace()
        }
    }

    fun fetchAndActivate(onComplete: ((Boolean) -> Unit)? = null) {
        Log.d(TAG, "🔄 fetchAndActivate manual...")
        remoteConfig?.fetchAndActivate()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "✅ Atualizado do Firebase: ${task.result}")
                    logAllValues()
                    onComplete?.invoke(true)
                } else {
                    Log.e(TAG, "⚠️ Erro ao atualizar", task.exception)
                    onComplete?.invoke(false)
                }
            }
    }

    private fun logAllValues() {
        Log.d(TAG, "")
        Log.d(TAG, "========================================")
        Log.d(TAG, "=== VALORES DO FIREBASE ===")
        Log.d(TAG, "enabled: ${remoteConfig?.getBoolean("enabled")}")
        Log.d(TAG, "title: ${remoteConfig?.getString("title")}")
        Log.d(TAG, "subtitle: ${remoteConfig?.getString("subtitle")}")
        Log.d(TAG, "wp1: ${remoteConfig?.getString("wp1")}")
        Log.d(TAG, "wp2: ${remoteConfig?.getString("wp2")}")
        Log.d(TAG, "wp3: ${remoteConfig?.getString("wp3")}")
        Log.d(TAG, "tg: ${remoteConfig?.getString("tg")}")
        Log.d(TAG, "========================================")
        Log.d(TAG, "")
    }

    fun isPremiumEnabled(): Boolean {
        return try {
            val value = remoteConfig?.getBoolean("enabled") ?: true
            Log.d(TAG, "📊 isPremiumEnabled = $value")
            value
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao obter enabled", e)
            true
        }
    }

    fun isMaintenanceMode(): Boolean {
        return try {
            remoteConfig?.getBoolean("maintenance_mode") ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter maintenance_mode", e)
            false
        }
    }

    fun shouldShowAds(): Boolean {
        return try {
            remoteConfig?.getBoolean("show_ads") ?: true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter show_ads", e)
            true
        }
    }

    fun getPremiumTitle(): String {
        return try {
            val value = remoteConfig?.getString("title") ?: "🎬 VERSÃO PREMIUM"
            Log.d(TAG, "📝 getPremiumTitle = $value")
            value
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter title", e)
            "🎬 VERSÃO PREMIUM"
        }
    }

    fun getPremiumSubtitle(): String {
        return try {
            val value = remoteConfig?.getString("subtitle") ?: "Desbloqueie todos os recursos premium!"
            Log.d(TAG, "📝 getPremiumSubtitle = $value")
            value
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter subtitle", e)
            "Desbloqueie todos os recursos premium!"
        }
    }

    fun getWhatsAppPhone1(): String {
        return try {
            val value = remoteConfig?.getString("wp1") ?: "5511999999999"
            Log.d(TAG, "📱 getWhatsAppPhone1 = $value")
            value
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter wp1", e)
            "5511999999999"
        }
    }

    fun getWhatsAppPhone2(): String {
        return try {
            val value = remoteConfig?.getString("wp2") ?: "5527995768269"
            Log.d(TAG, "📱 getWhatsAppPhone2 = $value")
            value
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter wp2", e)
            "5527995768269"
        }
    }

    fun getWhatsAppPhone3(): String {
        return try {
            val value = remoteConfig?.getString("wp3") ?: "5527997526988"
            Log.d(TAG, "📱 getWhatsAppPhone3 = $value")
            value
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter wp3", e)
            "5527997526988"
        }
    }

    fun getTelegramUsername(): String {
        return try {
            val value = remoteConfig?.getString("tg") ?: "Goldvipinforpodutos"
            Log.d(TAG, "✈️ getTelegramUsername = $value")
            value
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter tg", e)
            "Goldvipinforpodutos"
        }
    }

    fun getDebugInfo(): String {
        return try {
            remoteConfig?.getString("debug_info") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter debug_info", e)
            ""
        }
    }

    fun getString(key: String): String {
        return try {
            remoteConfig?.getString(key) ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter string '$key'", e)
            ""
        }
    }

    fun getBoolean(key: String): Boolean {
        return try {
            remoteConfig?.getBoolean(key) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter boolean '$key'", e)
            false
        }
    }

    fun getLong(key: String): Long {
        return try {
            remoteConfig?.getLong(key) ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter long '$key'", e)
            0L
        }
    }

    fun getDouble(key: String): Double {
        return try {
            remoteConfig?.getDouble(key) ?: 0.0
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter double '$key'", e)
            0.0
        }
    }

    fun isInitialized(): Boolean {
        val initialized = remoteConfig != null
        Log.d(TAG, "🔍 isInitialized = $initialized")
        return initialized
    }
}