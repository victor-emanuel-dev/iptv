package com.ivip.cineduostreammedia2026

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class MyApplication : Application() {

    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "========================================")
        Log.d(TAG, "=== APLICAÇÃO INICIANDO ===")
        Log.d(TAG, "========================================")

        // ========== 1. INICIALIZAR FIREBASE PRIMEIRO ========== //
        try {
            Log.d(TAG, "🔥 Verificando Firebase...")

            // Verificar se já está inicializado
            val existingApps = FirebaseApp.getApps(this)
            Log.d(TAG, "   Apps Firebase existentes: ${existingApps.size}")

            if (existingApps.isEmpty()) {
                Log.d(TAG, "   Inicializando Firebase pela primeira vez...")
                FirebaseApp.initializeApp(this)
                Log.d(TAG, "✅ Firebase inicializado com sucesso!")
            } else {
                Log.d(TAG, "✅ Firebase já estava inicializado")
                existingApps.forEach { app ->
                    Log.d(TAG, "   App: ${app.name}")
                }
            }

            // Confirmar que está funcionando
            val defaultApp = FirebaseApp.getInstance()
            Log.d(TAG, "📱 Firebase App: ${defaultApp.name}")
            Log.d(TAG, "🔑 Project ID: ${defaultApp.options.projectId}")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERRO CRÍTICO ao inicializar Firebase!", e)
            e.printStackTrace()
        }

        // ========== 2. DEPOIS INICIALIZAR REMOTE CONFIG ========== //
        try {
            Log.d(TAG, "🔧 Inicializando Remote Config...")
            RemoteConfigManager.initialize()
            Log.d(TAG, "✅ Remote Config inicializado")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao inicializar Remote Config", e)
            e.printStackTrace()
        }

        Log.d(TAG, "========================================")
        Log.d(TAG, "=== APLICAÇÃO PRONTA ===")
        Log.d(TAG, "========================================")
    }
}