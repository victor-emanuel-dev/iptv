package com.ivip.cineduostreammedia2026

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Configurar modo escuro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // ========== INICIALIZAR FIREBASE REMOTE CONFIG ========== //
        initializeRemoteConfig()

        // Ir direto para a tela de streaming (canais)
        navigateToStreaming()
    }

    /**
     * Inicializa o Firebase Remote Config
     * Busca configurações remotas em background
     */
    private fun initializeRemoteConfig() {
        try {
            Log.d(TAG, "🔧 Inicializando Firebase Remote Config...")

            // Inicializar Remote Config
            RemoteConfigManager.initialize()

            // Buscar novas configurações em background (não bloqueia a UI)
            RemoteConfigManager.fetchAndActivate { success ->
                if (success) {
                    Log.d(TAG, "✅ Configurações remotas atualizadas!")
                    Log.d(TAG, RemoteConfigManager.getDebugInfo())
                } else {
                    Log.d(TAG, "⚠️ Usando configurações em cache/padrão")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao inicializar Remote Config: ${e.message}", e)
            // App continua funcionando com valores padrão
        }
    }

    private fun navigateToStreaming() {
        val intent = Intent(this, StreamingActivity::class.java)
        startActivity(intent)
        finish()
    }
}