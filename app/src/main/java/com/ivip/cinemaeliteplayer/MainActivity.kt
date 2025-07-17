package com.ivip.cinemaeliteplayer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Instalar splash screen
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Configurar modo escuro
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Ir direto para a tela de streaming (canais)
        navigateToStreaming()
    }

    private fun navigateToStreaming() {
        val intent = Intent(this, StreamingActivity::class.java)
        startActivity(intent)
        finish()
    }
}