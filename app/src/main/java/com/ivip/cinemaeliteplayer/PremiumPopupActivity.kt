package com.ivip.cinemaeliteplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.ivip.cinemaeliteplayer.databinding.ActivityPremiumPopupBinding

class PremiumPopupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumPopupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        supportActionBar?.hide()
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        binding = ActivityPremiumPopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 90% tamanho, âncora topo, desloca 32dp para baixo
        val dm: DisplayMetrics = resources.displayMetrics
        window.setLayout((dm.widthPixels * 0.9).toInt(), (dm.heightPixels * 0.9).toInt())
        window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        val params = window.attributes
        params.y = (32 * dm.density).toInt() // deslocamento 32dp
        window.attributes = params

        setFinishOnTouchOutside(false)

        listeners()
    }

    private fun listeners() {
        binding.btnClose.setOnClickListener { finish() }
        binding.btnWhatsApp01.setOnClickListener { openWhatsApp("5527999109882") }
        binding.btnWhatsApp02.setOnClickListener { openWhatsApp("5527988911261") }
        binding.btnTelegram.setOnClickListener { openTelegram("+5527995097169") }
    }

    private fun openWhatsApp(phone: String) {
        runCatching {
            val url = "https://api.whatsapp.com/send?phone=$phone"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(this, "Instale o WhatsApp.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTelegram(phone: String) {
        runCatching {
            val url = "https://t.me/$phone"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            Toast.makeText(this, "Instale o Telegram.", Toast.LENGTH_SHORT).show()
        }
    }
}
