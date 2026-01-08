package com.ivip.tubitvpremium

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ivip.tubitvpremium.databinding.ActivityPremiumPopupBinding

class PremiumPopupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumPopupBinding

    companion object {
        private const val TAG = "PremiumPopup"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "=== PREMIUM POPUP INICIADO ===")

        // ========== TELA CHEIA ========== //
        setupFullScreen()

        try {
            if (!RemoteConfigManager.isInitialized()) {
                Log.w(TAG, "⚠️ Remote Config não inicializado!")
                RemoteConfigManager.initialize()

                Handler(Looper.getMainLooper()).postDelayed({
                    continueSetup()
                }, 1500)
            } else {
                continueSetup()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ ERRO no onCreate", e)
            finish()
        }
    }

    private fun setupFullScreen() {
        // Tela cheia imersiva
        WindowCompat.setDecorFitsSystemWindows(window, false)
        supportActionBar?.hide()

        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Ocultar barras do sistema
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun continueSetup() {
        try {
            Log.d(TAG, "🔧 Configurando UI...")

            binding = ActivityPremiumPopupBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupUI()
            fetchRemoteConfigUpdates()

            Log.d(TAG, "✅ POPUP PRONTO")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERRO em continueSetup", e)
            finish()
        }
    }

    private fun setupUI() {
        try {
            Log.d(TAG, "🎨 Configurando botões...")

            // ========== BOTÃO FECHAR ========== //
            binding.btnClose.setOnClickListener {
                Log.d(TAG, "❌ Fechando popup")
                finish()
            }

            // ========== TEXTOS ========== //
            binding.tvPremiumTitle.text = RemoteConfigManager.getPremiumTitle()
            binding.tvPremiumSubtitle.text = RemoteConfigManager.getPremiumSubtitle()

            // ========== BOTÕES WHATSAPP ========== //
            val phone1 = RemoteConfigManager.getWhatsAppPhone1()
            val phone2 = RemoteConfigManager.getWhatsAppPhone2()
            val phone3 = RemoteConfigManager.getWhatsAppPhone3()

            binding.btnWhatsApp01.apply {
                text = formatPhoneForDisplay(phone1)
                setOnClickListener { openWhatsApp(phone1) }
            }

            binding.btnWhatsApp02.apply {
                text = formatPhoneForDisplay(phone2)
                setOnClickListener { openWhatsApp(phone2) }
            }

            binding.btnWhatsApp03.apply {
                text = formatPhoneForDisplay(phone3)
                setOnClickListener { openWhatsApp(phone3) }
            }

            // ========== BOTÃO TELEGRAM ========== //
            val telegramUsername = RemoteConfigManager.getTelegramUsername()

            binding.btnTelegram02.apply {
                text = "@$telegramUsername"
                setOnClickListener { openTelegram(telegramUsername) }
            }

            Log.d(TAG, "✅ UI configurada")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro em setupUI", e)
        }
    }

    private fun fetchRemoteConfigUpdates() {
        RemoteConfigManager.fetchAndActivate { success ->
            runOnUiThread {
                Log.d(TAG, "📡 Config atualizado: $success")
            }
        }
    }

    private fun formatPhoneForDisplay(phone: String): String {
        return try {
            if (phone.startsWith("55") && phone.length >= 12) {
                val ddd = phone.substring(2, 4)
                val firstPart = phone.substring(4, 9)
                val secondPart = phone.substring(9)
                "+55 $ddd $firstPart-$secondPart"
            } else {
                phone
            }
        } catch (e: Exception) {
            phone
        }
    }

    private fun openWhatsApp(phone: String) {
        Log.d(TAG, "📱 Abrindo WhatsApp: $phone")
        runCatching {
            val message = "Olá! Gostaria de ativar o acesso premium no TubiTV Premium"
            val url = "https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(message)}"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { e ->
            Log.e(TAG, "❌ Erro ao abrir WhatsApp", e)
            Toast.makeText(this, "Instale o WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTelegram(username: String) {
        Log.d(TAG, "✈️ Abrindo Telegram: $username")
        runCatching {
            val url = "https://t.me/$username"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { e ->
            Log.e(TAG, "❌ Erro ao abrir Telegram", e)
            Toast.makeText(this, "Instale o Telegram", Toast.LENGTH_SHORT).show()
        }
    }
}