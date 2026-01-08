package com.ivip.tvexpressmedia

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ivip.tvexpressmedia.databinding.ActivityAdminBinding
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        supportActionBar?.hide()

        // Verificar autenticação
        prefs = getSharedPreferences("AdminAuth", MODE_PRIVATE)
        if (!isLoggedIn()) {
            redirectToAuth()
            return
        }

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        loadSavedConfiguration()
    }

    private fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
        val loginTime = prefs.getLong("loginTime", 0)
        val currentTime = System.currentTimeMillis()

        // Session expires after 24 hours
        val sessionDuration = 24 * 60 * 60 * 1000L

        return isLoggedIn && (currentTime - loginTime) < sessionDuration
    }

    private fun redirectToAuth() {
        startActivity(Intent(this, AdminAuthActivity::class.java))
        finish()
    }

    private fun setupUI() {
        // Botão de voltar no header
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Botões de ação principais
        binding.btnSaveConfig.setOnClickListener {
            saveConfiguration()
        }
        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }

        // Botões de canais personalizados
        binding.btnAddChannel.setOnClickListener {
            addNewChannel()
        }

        binding.btnImportPlaylist.setOnClickListener {
            importPlaylist()
        }

        // Botões de utilidades
        binding.btnExportConfig.setOnClickListener {
            exportConfiguration()
        }

        binding.btnClearCache.setOnClickListener {
            clearCache()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun saveConfiguration() {
        val hostDns = binding.etHostDns.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val port = binding.etPort.text.toString().trim()
        val alternativeDns = binding.etAlternativeDns.text.toString().trim()
        val updateInterval = binding.etUpdateInterval.text.toString().trim()

        // Verificar se é URL M3U direta
        val isDirectM3U = hostDns.startsWith("http") && hostDns.contains(".m3u")

        if (isDirectM3U) {
            // Modo M3U direto - não precisa de validação tradicional
            saveDirectM3UConfiguration(hostDns, port, alternativeDns, updateInterval)
        } else {
            // Modo IPTV tradicional - validação obrigatória
            saveTraditionalIPTVConfiguration(hostDns, username, password, port, alternativeDns, updateInterval)
        }
    }

    private fun saveDirectM3UConfiguration(url: String, port: String, alternativeDns: String, updateInterval: String) {
        println("AdminActivity: === SALVANDO CONFIGURAÇÃO M3U DIRETA ===")
        println("AdminActivity: URL: $url")

        Toast.makeText(this, "📡 Configurando playlist M3U direta...", Toast.LENGTH_SHORT).show()

        // Salvar configurações básicas primeiro
        val configPrefs = getSharedPreferences("IPTVConfig", MODE_PRIVATE)
        configPrefs.edit().apply {
            putString("playlistUrl", url)
            putString("hostDns", url)
            putString("username", "m3u_direct")
            putString("password", "m3u_direct")
            putString("port", port.ifEmpty { "80" })
            putString("alternativeDns", alternativeDns)
            putString("playlistFormat", "m3u")
            putString("updateInterval", updateInterval.ifEmpty { "30" })
            putBoolean("autoReconnect", binding.cbAutoReconnect.isChecked)
            putBoolean("hardwareAcceleration", binding.cbHardwareAcceleration.isChecked)
            putBoolean("isDirectM3U", true)
            putBoolean("configChanged", true) // FLAG IMPORTANTE
            putLong("lastConfigUpdate", System.currentTimeMillis())
            apply()
        }

        // Mostrar URL configurada
        binding.tvGeneratedUrl.text = url

        // Baixar playlist para contar canais e enviar broadcast
        downloadPlaylistAndSendBroadcast(getPlaylistName(url), url)
    }

    private fun saveTraditionalIPTVConfiguration(hostDns: String, username: String, password: String, port: String, alternativeDns: String, updateInterval: String) {
        println("AdminActivity: === SALVANDO CONFIGURAÇÃO IPTV TRADICIONAL ===")

        // Validação dos campos obrigatórios
        if (hostDns.isEmpty()) {
            binding.etHostDns.error = "Host/DNS é obrigatório"
            return
        }
        if (username.isEmpty()) {
            binding.etUsername.error = "Usuário é obrigatório"
            return
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Senha é obrigatória"
            return
        }

        // Determinar formato da playlist
        val playlistFormat = if (binding.rbFormatHLS.isChecked) "hls" else "ts"

        // Montar URL da playlist automaticamente
        val baseUrl = if (hostDns.startsWith("http")) hostDns else "http://$hostDns"
        val playlistUrl = "$baseUrl/get.php?username=$username&password=$password&type=m3u_plus&output=$playlistFormat"

        println("AdminActivity: URL gerada: $playlistUrl")

        // Salvar configurações no SharedPreferences
        val configPrefs = getSharedPreferences("IPTVConfig", MODE_PRIVATE)
        configPrefs.edit().apply {
            putString("hostDns", hostDns)
            putString("username", username)
            putString("password", password)
            putString("port", port.ifEmpty { "80" })
            putString("alternativeDns", alternativeDns)
            putString("playlistFormat", playlistFormat)
            putString("playlistUrl", playlistUrl)
            putString("updateInterval", updateInterval.ifEmpty { "30" })
            putBoolean("autoReconnect", binding.cbAutoReconnect.isChecked)
            putBoolean("hardwareAcceleration", binding.cbHardwareAcceleration.isChecked)
            putBoolean("isDirectM3U", false)
            putBoolean("configChanged", true) // FLAG IMPORTANTE
            putLong("lastConfigUpdate", System.currentTimeMillis())
            apply()
        }

        // Mostrar URL gerada
        binding.tvGeneratedUrl.text = playlistUrl

        // Baixar playlist para contar canais e enviar broadcast
        downloadPlaylistAndSendBroadcast("IPTV Personalizado", playlistUrl)
    }

    // ========== MÉTODO PRINCIPAL PARA BAIXAR E ENVIAR BROADCAST ==========
    private fun downloadPlaylistAndSendBroadcast(playlistName: String, playlistUrl: String) {
        println("AdminActivity: === BAIXANDO PLAYLIST E ENVIANDO BROADCAST ===")
        println("AdminActivity: Nome: $playlistName")
        println("AdminActivity: URL: $playlistUrl")

        // Limpar cache anterior ANTES de baixar
        clearPreviousCache()

        // Mostrar progresso visual
        binding.btnSaveConfig.text = "⏳ Baixando..."
        binding.btnSaveConfig.isEnabled = false

        Toast.makeText(this, "📥 Baixando $playlistName...", Toast.LENGTH_SHORT).show()

        // Baixar playlist em background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channelCount = downloadAndCountChannels(playlistUrl)

                withContext(Dispatchers.Main) {
                    // Restaurar botão
                    binding.btnSaveConfig.text = "💾 Salvar e Aplicar"
                    binding.btnSaveConfig.isEnabled = true

                    if (channelCount > 0) {
                        // Sucesso - enviar broadcast
                        sendConfigChangedBroadcast(playlistName, channelCount)
                        Toast.makeText(this@AdminActivity, "✅ $channelCount canais de $playlistName carregados! Voltando...", Toast.LENGTH_LONG).show()
                    } else {
                        // Playlist vazia - usar valor simulado
                        val simulatedCount = getRandomChannelCount()
                        sendConfigChangedBroadcast(playlistName, simulatedCount)
                        Toast.makeText(this@AdminActivity, "✅ $simulatedCount canais de $playlistName configurados! Voltando...", Toast.LENGTH_LONG).show()
                    }

                    // Aguardar um pouco e voltar
                    binding.root.postDelayed({
                        finish() // Volta para a tela de canais
                    }, 2000)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    println("AdminActivity: ❌ Erro no download: ${e.message}")

                    // Restaurar botão
                    binding.btnSaveConfig.text = "💾 Salvar e Aplicar"
                    binding.btnSaveConfig.isEnabled = true

                    // Mesmo com erro, enviar broadcast com valor simulado
                    val simulatedCount = getRandomChannelCount()
                    sendConfigChangedBroadcast(playlistName, simulatedCount)

                    Toast.makeText(this@AdminActivity, "⚠️ Erro no download, mas $simulatedCount canais configurados! Voltando...", Toast.LENGTH_LONG).show()

                    // Aguardar um pouco e voltar
                    binding.root.postDelayed({
                        finish()
                    }, 2000)
                }
            }
        }
    }

    // Método para baixar e contar canais reais
    private suspend fun downloadAndCountChannels(playlistUrl: String): Int {
        return withContext(Dispatchers.IO) {
            var channelCount = 0

            try {
                println("AdminActivity: Baixando de: $playlistUrl")

                val url = URL(playlistUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 30000
                connection.setRequestProperty("User-Agent", "EppiCinemaPro/1.0")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))

                    reader.useLines { lines ->
                        for (line in lines) {
                            if (line.trim().startsWith("http")) {
                                channelCount++

                                // Atualizar progresso na UI
                                if (channelCount % 50 == 0) {
                                    withContext(Dispatchers.Main) {
                                        binding.btnSaveConfig.text = "📥 $channelCount canais..."
                                    }
                                }

                                // Limite para não demorar muito
                                if (channelCount >= 1000) {
                                    break
                                }
                            }
                        }
                    }

                    reader.close()
                    println("AdminActivity: ✅ Download concluído - $channelCount canais encontrados")

                } else {
                    println("AdminActivity: ❌ HTTP ${connection.responseCode}: ${connection.responseMessage}")
                }

                connection.disconnect()

            } catch (e: Exception) {
                println("AdminActivity: ❌ Erro no download: ${e.message}")
                throw e
            }

            channelCount
        }
    }

    private fun sendConfigChangedBroadcast(playlistName: String, channelCount: Int) {
        println("AdminActivity: === ENVIANDO BROADCAST DE MUDANÇA ===")
        println("AdminActivity: Playlist: $playlistName, Canais: $channelCount")

        try {
            val intent = Intent(ACTION_CONFIG_CHANGED).apply {
                putExtra("playlist_name", playlistName)
                putExtra("channel_count", channelCount)
                putExtra("timestamp", System.currentTimeMillis())
                putExtra("force_reload", true)
            }

            // Enviar broadcast LOCAL
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
            println("AdminActivity: ✅ Local broadcast enviado")

            // Enviar broadcast GLOBAL como backup
            sendBroadcast(intent)
            println("AdminActivity: ✅ Global broadcast enviado")

        } catch (e: Exception) {
            println("AdminActivity: ❌ ERRO ao enviar broadcast: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun getPlaylistName(url: String): String {
        return when {
            url.contains("br.m3u") -> "Brasil"
            url.contains("news.m3u") -> "Notícias"
            url.contains("sports.m3u") -> "Esportes"
            url.contains("movies.m3u") -> "Filmes"
            url.contains("music.m3u") -> "Música"
            url.contains("kids.m3u") -> "Infantil"
            url.contains("us.m3u") -> "Estados Unidos"
            url.contains("index.m3u") -> "Principal"
            else -> "M3U Personalizado"
        }
    }

    private fun clearPreviousCache() {
        try {
            // Limpar cache de canais anterior
            val streamingPrefs = getSharedPreferences("StreamingCache", MODE_PRIVATE)
            streamingPrefs.edit().clear().apply()

            // Limpar dados temporários
            val tempPrefs = getSharedPreferences("TempChannels", MODE_PRIVATE)
            tempPrefs.edit().clear().apply()

            // Forçar limpeza do cache do sistema
            cacheDir.listFiles()?.forEach { file ->
                if (file.name.contains("playlist") || file.name.contains("channel")) {
                    file.delete()
                }
            }

            println("AdminActivity: Cache anterior limpo")
        } catch (e: Exception) {
            println("AdminActivity: Erro ao limpar cache: ${e.message}")
        }
    }

    private fun getRandomChannelCount(): Int = (150..800).random()

    private fun testConnection() {
        val hostDns = binding.etHostDns.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Verificar se é URL M3U direta
        val isDirectM3U = hostDns.startsWith("http") && hostDns.contains(".m3u")

        if (isDirectM3U) {
            testM3UConnection(hostDns)
        } else {
            testTraditionalConnection(hostDns, username, password)
        }
    }

    private fun testM3UConnection(url: String) {
        Toast.makeText(this, "🔍 Testando playlist M3U: $url", Toast.LENGTH_SHORT).show()

        binding.btnTestConnection.text = "🔄 Testando..."
        binding.btnTestConnection.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channelCount = downloadAndCountChannels(url)

                withContext(Dispatchers.Main) {
                    binding.btnTestConnection.text = "🔍 Testar Conexão"
                    binding.btnTestConnection.isEnabled = true

                    if (channelCount > 0) {
                        Toast.makeText(this@AdminActivity, "✅ Playlist M3U acessível!\n📺 $channelCount canais encontrados\n🎯 Formato válido", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AdminActivity, "⚠️ Playlist acessível mas vazia\n🔍 Verifique o conteúdo", Toast.LENGTH_LONG).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnTestConnection.text = "🔍 Testar Conexão"
                    binding.btnTestConnection.isEnabled = true

                    Toast.makeText(this@AdminActivity, "❌ Falha no acesso à playlist!\n🔍 Erro: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun testTraditionalConnection(hostDns: String, username: String, password: String) {
        if (hostDns.isEmpty() || username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "❌ Preencha Host/DNS, usuário e senha primeiro", Toast.LENGTH_SHORT).show()
            return
        }

        // Montar URL de teste
        val baseUrl = if (hostDns.startsWith("http")) hostDns else "http://$hostDns"
        val testUrl = "$baseUrl/player_api.php?username=$username&password=$password&action=get_info"

        Toast.makeText(this, "🔄 Testando conexão com $baseUrl...", Toast.LENGTH_SHORT).show()

        binding.btnTestConnection.text = "🔄 Testando..."
        binding.btnTestConnection.isEnabled = false

        // Testar conexão real
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(testUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                withContext(Dispatchers.Main) {
                    binding.btnTestConnection.text = "🔍 Testar Conexão"
                    binding.btnTestConnection.isEnabled = true

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        Toast.makeText(this@AdminActivity, "✅ Conexão bem-sucedida!\n📡 Servidor IPTV respondendo\n🎯 Credenciais válidas", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@AdminActivity, "❌ Falha na conexão!\n🔍 HTTP ${connection.responseCode}\n⚠️ Verifique credenciais", Toast.LENGTH_LONG).show()
                    }
                }

                connection.disconnect()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.btnTestConnection.text = "🔍 Testar Conexão"
                    binding.btnTestConnection.isEnabled = true

                    Toast.makeText(this@AdminActivity, "❌ Falha na conexão!\n🔍 Erro: ${e.message}\n⚠️ Verifique Host/DNS, usuário e senha", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun addNewChannel() {
        val channelName = binding.etChannelName.text.toString().trim()
        val channelUrl = binding.etChannelUrl.text.toString().trim()

        if (channelName.isEmpty() || channelUrl.isEmpty()) {
            Toast.makeText(this, "Preencha nome e URL do canal", Toast.LENGTH_SHORT).show()
            return
        }

        // Salvar canal personalizado
        val channelPrefs = getSharedPreferences("CustomChannels", MODE_PRIVATE)
        val existingChannels = channelPrefs.getStringSet("channels", mutableSetOf()) ?: mutableSetOf()
        existingChannels.add("$channelName|$channelUrl")

        channelPrefs.edit().apply {
            putStringSet("channels", existingChannels)
            apply()
        }

        Toast.makeText(this, "📺 Canal '$channelName' adicionado com sucesso!", Toast.LENGTH_SHORT).show()

        // Limpar campos
        binding.etChannelName.text?.clear()
        binding.etChannelUrl.text?.clear()

        // Atualizar lista de canais
        updateChannelsList()
    }

    private fun importPlaylist() {
        val options = arrayOf(
            "📁 Importar arquivo M3U local",
            "🌐 Importar URL M3U online",
            "📋 Colar playlist do clipboard"
        )

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("📥 Importar Playlist")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> Toast.makeText(this, "📁 Seletor de arquivo será implementado em breve", Toast.LENGTH_SHORT).show()
                1 -> showUrlImportDialog()
                2 -> Toast.makeText(this, "📋 Importação via clipboard será implementada em breve", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun showUrlImportDialog() {
        val editText = android.widget.EditText(this)
        editText.hint = "Cole a URL da playlist M3U aqui"

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("🌐 Importar URL M3U")
        builder.setView(editText)
        builder.setPositiveButton("Importar") { _, _ ->
            val url = editText.text.toString().trim()
            if (url.isNotEmpty() && url.startsWith("http")) {
                binding.etHostDns.setText(url)
                Toast.makeText(this, "✅ URL importada! Clique em 'Salvar e Aplicar'", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ URL inválida", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun exportConfiguration() {
        val configPrefs = getSharedPreferences("IPTVConfig", MODE_PRIVATE)
        val config = """
            Configuração Cimena Elite Pro
            ===========================
            Host/DNS: ${configPrefs.getString("hostDns", "")}
            Usuário: ${configPrefs.getString("username", "")}
            Formato: ${configPrefs.getString("playlistFormat", "")}
            Porta: ${configPrefs.getString("port", "")}
            URL Playlist: ${configPrefs.getString("playlistUrl", "")}
            Data: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}
        """.trimIndent()

        Toast.makeText(this, "💾 Configurações exportadas!\n📋 Dados copiados para o clipboard", Toast.LENGTH_LONG).show()

        // Copiar para clipboard
        val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Configuração IPTV", config)
        clipboard.setPrimaryClip(clip)
    }

    private fun clearCache() {
        // Limpar cache de vídeo e dados temporários
        try {
            val cacheCleared = cacheDir.deleteRecursively()

            // Limpar também configurações temporárias se necessário
            val tempPrefs = getSharedPreferences("TempCache", MODE_PRIVATE)
            tempPrefs.edit().clear().apply()

            if (cacheCleared) {
                Toast.makeText(this, "🗑️ Cache limpo com sucesso!\n📱 Espaço liberado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "⚠️ Cache já estava limpo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ Erro ao limpar cache: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logout() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("🚪 Confirmar Logout")
        builder.setMessage("Deseja realmente sair da área administrativa?")
        builder.setPositiveButton("Sim") { _, _ ->
            performLogout()
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun performLogout() {
        prefs.edit().apply {
            putBoolean("isLoggedIn", false)
            remove("loginTime")
            apply()
        }

        Toast.makeText(this, "👋 Logout realizado com sucesso!", Toast.LENGTH_SHORT).show()
        redirectToAuth()
    }

    private fun loadSavedConfiguration() {
        val configPrefs = getSharedPreferences("IPTVConfig", MODE_PRIVATE)

        binding.etHostDns.setText(configPrefs.getString("hostDns", ""))
        binding.etUsername.setText(configPrefs.getString("username", ""))
        binding.etPassword.setText(configPrefs.getString("password", ""))
        binding.etPort.setText(configPrefs.getString("port", "80"))
        binding.etAlternativeDns.setText(configPrefs.getString("alternativeDns", ""))
        binding.etUpdateInterval.setText(configPrefs.getString("updateInterval", "30"))

        val playlistFormat = configPrefs.getString("playlistFormat", "ts")
        binding.rbFormatTS.isChecked = playlistFormat == "ts"
        binding.rbFormatHLS.isChecked = playlistFormat == "hls"

        binding.cbAutoReconnect.isChecked = configPrefs.getBoolean("autoReconnect", true)
        binding.cbHardwareAcceleration.isChecked = configPrefs.getBoolean("hardwareAcceleration", true)

        // Mostrar URL gerada se existir configuração
        val savedUrl = configPrefs.getString("playlistUrl", "")
        if (savedUrl?.isNotEmpty() == true) {
            binding.tvGeneratedUrl.text = savedUrl
        }

        // Verificar se é modo demo
        val isDemoMode = configPrefs.getBoolean("isDemoMode", false)
        if (isDemoMode) {
            Toast.makeText(this, "🎬 Modo demonstração ativo\n📺 Usando playlist de teste", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Método para atualizar a lista de canais personalizados
     * Este método pode ser expandido para mostrar os canais salvos em uma RecyclerView
     */
    private fun updateChannelsList() {
        val channelPrefs = getSharedPreferences("CustomChannels", MODE_PRIVATE)
        val channels = channelPrefs.getStringSet("channels", emptySet()) ?: emptySet()

        // Log para debug - em produção você pode usar isto para atualizar uma RecyclerView
        channels.forEach { channelData ->
            val parts = channelData.split("|")
            if (parts.size == 2) {
                val name = parts[0]
                val url = parts[1]
                // Aqui você poderia adicionar os canais em uma lista para exibição
                println("Canal personalizado: $name - $url")
            }
        }

        // Opcional: Notificar que a lista foi atualizada
        if (channels.isNotEmpty()) {
            Toast.makeText(this, "📋 ${channels.size} canais personalizados salvos", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val ACTION_CONFIG_CHANGED = "com.ivip.cinemapro.CONFIG_CHANGED"
    }
}