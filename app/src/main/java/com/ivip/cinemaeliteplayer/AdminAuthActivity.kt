package com.ivip.cinemaeliteplayer

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.ivip.cinemaeliteplayer.databinding.ActivityAdminAuthBinding

class AdminAuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminAuthBinding
    private lateinit var prefs: SharedPreferences

    // Credenciais de administrador (em produção, usar hash/criptografia)
    private val ADMIN_USERNAME = "admin"
    private val ADMIN_PASSWORD = "admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        supportActionBar?.hide()

        binding = ActivityAdminAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("AdminAuth", MODE_PRIVATE)

        // Verificar se já está logado
        if (isLoggedIn()) {
            navigateToAdmin()
            return
        }

        setupUI()
    }

    private fun setupUI() {
        // Botão de voltar
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (username == ADMIN_USERNAME && password == ADMIN_PASSWORD) {
            // Login bem-sucedido
            saveLoginState()
            Toast.makeText(this, "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
            navigateToAdmin()
        } else {
            Toast.makeText(this, "Credenciais inválidas", Toast.LENGTH_SHORT).show()
            binding.etPassword.text?.clear()
        }
    }

    private fun saveLoginState() {
        prefs.edit().apply {
            putBoolean("isLoggedIn", true)
            putLong("loginTime", System.currentTimeMillis())
            apply()
        }
    }

    private fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val loginTime = prefs.getLong("loginTime", 0)
        val currentTime = System.currentTimeMillis()

        // Session expires after 24 hours
        val sessionDuration = 24 * 60 * 60 * 1000L // 24 hours in milliseconds

        return isLoggedIn && (currentTime - loginTime) < sessionDuration
    }

    private fun navigateToAdmin() {
        startActivityForResult(Intent(this, AdminActivity::class.java), 1001)
        finish()
    }
}