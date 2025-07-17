package com.ivip.cinemaeliteplayer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultDataSource
import com.ivip.cinemaeliteplayer.R
import com.ivip.cinemaeliteplayer.databinding.ActivityStreamingBinding
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class Channel(
    val id: Int,
    val name: String,
    val description: String,
    val url: String,
    val category: String,
    val logo: String? = null,
    val isLive: Boolean = true
)

class StreamingActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null
    private lateinit var binding: ActivityStreamingBinding
    private var isFullScreen: Boolean = false
    private var isControlsVisible: Boolean = false
    private val hideControlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }

    private lateinit var channelsAdapter: ChannelsAdapter
    private var currentChannel: Channel? = null
    private var isChannelsTabActive = true
    private var channelsList = mutableListOf<Channel>()
    private var filteredChannelsList = mutableListOf<Channel>()

    // Broadcast receivers
    private lateinit var configChangeReceiver: BroadcastReceiver
    private lateinit var globalReceiver: BroadcastReceiver

    companion object {
        const val ACTION_CONFIG_CHANGED = "com.ivip.cinemaeliteplayer.CONFIG_CHANGED"

        // 5 CANAIS GRATUITOS COM URLS TESTADAS E FUNCIONAIS
        val DEFAULT_FREE_CHANNELS = listOf(
            Channel(1, "Red Bull TV", "Esportes radicais e eventos", "https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8", "Esportes", isLive = true),
            Channel(2, "RT News", "Russia Today - Notícias 24/7", "https://rt-glb.rttv.com/live/rtnews/playlist.m3u8", "Notícias", isLive = true),
            Channel(3, "Al Jazeera English", "Canal de notícias internacional", "https://live-hls-web-aje.getaj.net/AJE/index.m3u8", "Notícias", isLive = true),
            Channel(4, "Fashion TV", "Moda e estilo de vida", "https://fashiontv-fashiontv-1-eu.rakuten.wurl.tv/playlist.m3u8", "Estilo", isLive = true),
            Channel(5, "Bloomberg TV", "Notícias financeiras", "https://bloomberg.com/media-manifest/streams/phoenix-us.m3u8", "Economia", isLive = true),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        supportActionBar?.hide()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityStreamingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        println("StreamingActivity: === INICIANDO STREAMING ACTIVITY ===")

        // IMPORTANTE: Inicializar player PRIMEIRO
        initializePlayer()

        // Configurar broadcast receivers
        setupBroadcastReceiver()

        setupUI()
        setupRecyclerView()

        // Verificar se há nova configuração ao iniciar
        checkForNewConfiguration()
    }

    // ========== MÉTODO PARA ABRIR CONFIGURAÇÕES ADMIN ==========
    private fun openAdminSettings() {
        println("StreamingActivity: === ABRINDO CONFIGURAÇÕES ADMINISTRATIVAS ===")
        try {
            val intent = Intent(this, AdminAuthActivity::class.java)
            startActivity(intent)
            println("StreamingActivity: ✅ AdminAuthActivity iniciada com sucesso")
        } catch (e: Exception) {
            println("StreamingActivity: ❌ Erro ao abrir AdminAuthActivity: ${e.message}")
            Toast.makeText(this, "❌ Erro ao abrir configurações: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializePlayer() {
        println("StreamingActivity: === INICIALIZANDO EXOPLAYER ===")

        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(this).build()

            // Conectar player ao PlayerView
            binding.playerView.player = exoPlayer

            // Configurar listener para eventos
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    println("StreamingActivity: Estado do player mudou para: $playbackState")

                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.tvStatus.text = "🔄 Carregando..."
                            println("StreamingActivity: Player BUFFERING")
                        }
                        Player.STATE_READY -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStatus.text = "📺 AO VIVO"
                            println("StreamingActivity: Player READY - vídeo pronto para reproduzir")
                        }
                        Player.STATE_ENDED -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStatus.text = "⏹️ Transmissão finalizada"
                            println("StreamingActivity: Player ENDED")
                        }
                        Player.STATE_IDLE -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStatus.text = "⚠️ Aguardando..."
                            println("StreamingActivity: Player IDLE")
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = "❌ Erro na transmissão"

                    val errorMessage = when (error.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "Falha na conexão de rede"
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Timeout de conexão"
                        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "Formato inválido"
                        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "Playlist inválida"
                        else -> "Erro: ${error.message}"
                    }

                    println("StreamingActivity: ERRO NO PLAYER: $errorMessage")
                    Toast.makeText(this@StreamingActivity, "❌ $errorMessage\n\n⏭️ Tentando próximo canal...", Toast.LENGTH_LONG).show()

                    // Tentar próximo canal automaticamente
                    tryNextValidChannel()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    binding.btnPlayPause.setImageResource(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_modern
                    )

                    if (isPlaying) {
                        println("StreamingActivity: Vídeo está REPRODUZINDO")
                    } else {
                        println("StreamingActivity: Vídeo está PAUSADO")
                    }
                }
            })

            println("StreamingActivity: ExoPlayer inicializado com sucesso")
        }
    }

    private fun setupBroadcastReceiver() {
        println("StreamingActivity: === CONFIGURANDO BROADCAST RECEIVER ===")

        // Receiver local (LocalBroadcastManager) - SEMPRE FUNCIONA
        configChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                println("StreamingActivity: === BROADCAST LOCAL RECEBIDO ===")
                if (intent?.action == ACTION_CONFIG_CHANGED) {
                    val playlistName = intent.getStringExtra("playlist_name") ?: "Nova Playlist"
                    val channelCount = intent.getIntExtra("channel_count", 0)
                    val timestamp = intent.getLongExtra("timestamp", 0)
                    val forceReload = intent.getBooleanExtra("force_reload", true)

                    println("StreamingActivity: Playlist: $playlistName")
                    println("StreamingActivity: Canais: $channelCount")
                    println("StreamingActivity: Timestamp: $timestamp")
                    println("StreamingActivity: Force reload: $forceReload")

                    if (forceReload) {
                        Toast.makeText(this@StreamingActivity, "📡 $playlistName recebida! Atualizando...", Toast.LENGTH_SHORT).show()
                        forceCompleteReloadFromBroadcast()
                    }
                }
            }
        }

        // Receiver global com flag RECEIVER_NOT_EXPORTED (Android 14+)
        globalReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                println("StreamingActivity: === BROADCAST GLOBAL RECEBIDO ===")
                if (intent?.action == ACTION_CONFIG_CHANGED) {
                    val playlistName = intent.getStringExtra("playlist_name") ?: "Nova Playlist"
                    Toast.makeText(this@StreamingActivity, "📡 $playlistName (global) recebida! Atualizando...", Toast.LENGTH_SHORT).show()
                    forceCompleteReloadFromBroadcast()
                }
            }
        }

        // Registrar LOCAL receiver (sempre funciona)
        LocalBroadcastManager.getInstance(this).registerReceiver(
            configChangeReceiver,
            IntentFilter(ACTION_CONFIG_CHANGED)
        )

        // Registrar GLOBAL receiver com flag para Android 14+
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+) - usar RECEIVER_NOT_EXPORTED
                registerReceiver(
                    globalReceiver,
                    IntentFilter(ACTION_CONFIG_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
                )
                println("StreamingActivity: Global receiver registrado com RECEIVER_NOT_EXPORTED")
            } else {
                // Android 12 e anteriores - método tradicional
                registerReceiver(globalReceiver, IntentFilter(ACTION_CONFIG_CHANGED))
                println("StreamingActivity: Global receiver registrado (método tradicional)")
            }
        } catch (e: Exception) {
            println("StreamingActivity: ⚠️ Erro ao registrar global receiver: ${e.message}")
            println("StreamingActivity: 📡 Usando apenas LocalBroadcastManager")
        }

        println("StreamingActivity: Broadcast receivers configurados")
    }

    private fun checkForNewConfiguration() {
        println("StreamingActivity: === VERIFICANDO NOVA CONFIGURAÇÃO ===")

        val configPrefs = getSharedPreferences("IPTVConfig", MODE_PRIVATE)
        val configChanged = configPrefs.getBoolean("configChanged", false)
        val playlistUrl = configPrefs.getString("playlistUrl", "")

        if (configChanged && !playlistUrl.isNullOrEmpty()) {
            println("StreamingActivity: Nova configuração detectada: $playlistUrl")
            Toast.makeText(this, "🔄 Nova configuração detectada! Carregando...", Toast.LENGTH_SHORT).show()
            forceCompleteReloadFromBroadcast()
        } else {
            println("StreamingActivity: Nenhuma nova configuração - carregando canais padrão")
            loadDefaultFreeChannels()
        }
    }

    private fun forceCompleteReloadFromBroadcast() {
        println("StreamingActivity: === FORÇANDO RECARREGAMENTO COMPLETO ===")

        runOnUiThread {
            try {
                // 1. Parar player e limpar tudo
                println("StreamingActivity: Parando player e limpando dados")
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                channelsList.clear()
                filteredChannelsList.clear()
                currentChannel = null

                // 2. Limpar UI
                binding.recyclerChannels.adapter = null
                binding.recyclerChannels.layoutManager = null
                binding.tvChannelName.text = ""
                binding.tvChannelDescription.text = ""
                binding.tvStatus.text = "🔄 Carregando nova playlist..."

                // 3. Ler nova configuração
                val configPrefs = getSharedPreferences("IPTVConfig", MODE_PRIVATE)
                val newPlaylistUrl = configPrefs.getString("playlistUrl", "")
                val isDirectM3U = configPrefs.getBoolean("isDirectM3U", false)
                val configChanged = configPrefs.getBoolean("configChanged", false)

                println("StreamingActivity: Nova URL: $newPlaylistUrl")
                println("StreamingActivity: É M3U direto: $isDirectM3U")
                println("StreamingActivity: Config mudou: $configChanged")

                // 4. Reconfigurar RecyclerView
                binding.recyclerChannels.layoutManager = LinearLayoutManager(this)

                // 5. Carregar nova playlist OU voltar aos canais gratuitos
                if (!newPlaylistUrl.isNullOrEmpty() && configChanged) {
                    println("StreamingActivity: Carregando nova playlist: $newPlaylistUrl")

                    if (isDirectM3U || newPlaylistUrl.contains(".m3u")) {
                        loadExternalM3UPlaylist(newPlaylistUrl)
                    } else {
                        loadIPTVPlaylist(newPlaylistUrl)
                    }

                    // Marcar config como processada
                    configPrefs.edit().putBoolean("configChanged", false).apply()

                } else {
                    println("StreamingActivity: Nenhuma nova configuração - mantendo canais gratuitos")
                    loadDefaultFreeChannels()
                }

                // 6. Voltar para aba de canais
                switchToChannelsTab()
                Toast.makeText(this, "✅ Playlist atualizada com sucesso!", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                println("StreamingActivity: ❌ ERRO no recarregamento: ${e.message}")
                e.printStackTrace()

                // Em caso de erro, voltar aos canais gratuitos
                loadDefaultFreeChannels()
                Toast.makeText(this, "⚠️ Erro na atualização - voltando aos canais gratuitos", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ========== MÉTODOS PARA CARREGAR PLAYLISTS EXTERNAS ==========
    private fun loadExternalM3UPlaylist(playlistUrl: String) {
        println("StreamingActivity: === CARREGANDO PLAYLIST M3U EXTERNA ===")
        println("StreamingActivity: URL: $playlistUrl")

        showLoadingState()
        binding.tvStatus.text = "📥 Baixando playlist..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channels = downloadAndParseM3U(playlistUrl)

                withContext(Dispatchers.Main) {
                    if (channels.isNotEmpty()) {
                        println("StreamingActivity: ✅ ${channels.size} canais carregados da playlist externa")

                        // Atualizar listas
                        channelsList.clear()
                        filteredChannelsList.clear()
                        channelsList.addAll(channels)
                        filteredChannelsList.addAll(channels)

                        // Recriar adapter
                        channelsAdapter = ChannelsAdapter(filteredChannelsList) { channel -> selectChannel(channel) }
                        binding.recyclerChannels.adapter = channelsAdapter

                        // Selecionar primeiro canal
                        if (channelsList.isNotEmpty()) {
                            val firstChannel = channelsList.first()
                            currentChannel = firstChannel
                            updateChannelInfo()
                            loadChannel(firstChannel.url)
                        }

                        binding.progressBar.visibility = View.GONE
                        binding.tvStatus.text = "📺 AO VIVO"

                        Toast.makeText(this@StreamingActivity, "🎉 ${channels.size} canais carregados da nova playlist!", Toast.LENGTH_LONG).show()

                    } else {
                        println("StreamingActivity: ⚠️ Playlist externa vazia")
                        Toast.makeText(this@StreamingActivity, "⚠️ Playlist vazia - voltando aos canais gratuitos", Toast.LENGTH_LONG).show()
                        loadDefaultFreeChannels()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    println("StreamingActivity: ❌ Erro ao carregar playlist externa: ${e.message}")
                    e.printStackTrace()

                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = "❌ Erro ao carregar"
                    Toast.makeText(this@StreamingActivity, "❌ Erro na playlist externa - voltando aos canais gratuitos", Toast.LENGTH_LONG).show()
                    loadDefaultFreeChannels()
                }
            }
        }
    }

    private fun loadIPTVPlaylist(playlistUrl: String) {
        println("StreamingActivity: === CARREGANDO PLAYLIST IPTV ===")
        println("StreamingActivity: URL: $playlistUrl")

        // Por simplicidade, tratar como M3U direto
        loadExternalM3UPlaylist(playlistUrl)
    }

    // Método para baixar e processar M3U
    private suspend fun downloadAndParseM3U(playlistUrl: String): List<Channel> {
        return withContext(Dispatchers.IO) {
            val channels = mutableListOf<Channel>()

            try {
                println("StreamingActivity: Baixando M3U de: $playlistUrl")

                val url = URL(playlistUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.setRequestProperty("User-Agent", "CinemaElitePlayer/1.0")
                connection.setRequestProperty("Accept", "*/*")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream), 8192)

                    var channelId = 1
                    var currentChannelName = ""
                    var currentChannelGroup = "Geral"
                    var lineCount = 0
                    var channelCount = 0

                    reader.useLines { lines ->
                        for (line in lines) {
                            lineCount++
                            val trimmedLine = line.trim()

                            // Atualizar progresso a cada 1000 linhas
                            if (lineCount % 1000 == 0) {
                                withContext(Dispatchers.Main) {
                                    binding.tvStatus.text = "📥 Processando... $channelCount canais"
                                }
                            }

                            if (trimmedLine.startsWith("#EXTINF:")) {
                                // Extrair nome do canal
                                currentChannelName = extractChannelNameFromLine(trimmedLine)
                                currentChannelGroup = extractChannelGroupFromLine(trimmedLine)

                            } else if (trimmedLine.startsWith("http") && currentChannelName.isNotEmpty()) {
                                // URL do canal - criar objeto Channel
                                val channel = Channel(
                                    id = channelId++,
                                    name = currentChannelName.ifEmpty { "Canal $channelId" },
                                    description = "Canal $currentChannelGroup",
                                    url = trimmedLine,
                                    category = currentChannelGroup.ifEmpty { "Geral" },
                                    isLive = true
                                )
                                channels.add(channel)
                                channelCount++

                                // Log apenas primeiros canais
                                if (channelCount <= 5) {
                                    println("StreamingActivity: Canal $channelCount: $currentChannelName")
                                }

                                currentChannelName = ""
                            }

                            // Limite para não travar
                            if (channelCount >= 1000) {
                                println("StreamingActivity: Limite de 1000 canais atingido")
                                break
                            }
                        }
                    }

                    reader.close()
                    println("StreamingActivity: ✅ Processamento concluído - $channelCount canais")

                } else {
                    throw Exception("HTTP ${connection.responseCode}: ${connection.responseMessage}")
                }

                connection.disconnect()

            } catch (e: Exception) {
                println("StreamingActivity: ❌ Erro no download: ${e.message}")
                throw Exception("Falha ao baixar playlist: ${e.message}")
            }

            channels
        }
    }

    private fun extractChannelNameFromLine(line: String): String {
        return try {
            val nameStart = line.lastIndexOf(',')
            if (nameStart != -1 && nameStart < line.length - 1) {
                line.substring(nameStart + 1).trim()
            } else {
                "Canal ${System.currentTimeMillis() % 1000}"
            }
        } catch (e: Exception) {
            "Canal Desconhecido"
        }
    }

    private fun extractChannelGroupFromLine(line: String): String {
        return try {
            val groupStart = line.indexOf("group-title=\"")
            if (groupStart != -1) {
                val start = groupStart + 13
                val end = line.indexOf("\"", start)
                if (end != -1) {
                    return line.substring(start, end)
                }
            }
            "Geral"
        } catch (e: Exception) {
            "Geral"
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerChannels.apply {
            layoutManager = LinearLayoutManager(this@StreamingActivity)
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            // FORÇAR REMOÇÃO DE QUALQUER PADDING/MARGIN
            setPadding(0, 0, 0, 0)
            clipToPadding = false

            // FORÇAR MARGEM ZERO NOS LAYOUTPARAMS
            val layoutParams = layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.setMargins(0, 0, 0, 0)
            this.layoutParams = layoutParams

            // REMOVER QUALQUER DECORAÇÃO
            while (itemDecorationCount > 0) {
                removeItemDecorationAt(0)
            }
        }

        // FORÇAR REMOÇÃO TOTAL DE PADDING/MARGIN DO PLAYER
        binding.playerContainer.post {
            binding.playerContainer.setPadding(0, 0, 0, 0)
            val playerParams = binding.playerContainer.layoutParams as LinearLayout.LayoutParams
            playerParams.setMargins(0, 0, 0, 0)
            binding.playerContainer.layoutParams = playerParams

            // FORÇAR REMOÇÃO DE PADDING DO PLAYERVIEW TAMBÉM
            binding.playerView.setPadding(0, 0, 0, 0)
        }

        // APLICAR CONFIGURAÇÕES CUSTOMIZADAS
        applySidePanelConfiguration()
    }

    private fun applySidePanelConfiguration() {
        binding.sidePanel.post {
            val sidePanelParams = binding.sidePanel.layoutParams as LinearLayout.LayoutParams
            sidePanelParams.leftMargin = -16 // Margem negativa para sobrepor
            binding.sidePanel.layoutParams = sidePanelParams

            // USAR WINDOWINSETS PARA CALCULAR PADDING CORRETO
            binding.sidePanel.setOnApplyWindowInsetsListener { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                val navigationBarWidth = systemBars.right
                val additionalPadding = (16 * resources.displayMetrics.density).toInt() // 16dp extra
                val totalRightPadding = navigationBarWidth + additionalPadding

                binding.sidePanel.setPadding(8, 0, totalRightPadding, 0)

                println("StreamingActivity: WindowInsets - navigationBarWidth: $navigationBarWidth, totalPadding: $totalRightPadding")

                insets
            }

            // FALLBACK: Se WindowInsets não funcionar, usar padding maior
            val fallbackPadding = (48 * resources.displayMetrics.density).toInt() // 48dp
            binding.sidePanel.setPadding(8, 0, fallbackPadding, 0)

            // FORÇAR ATUALIZAÇÃO DO LAYOUT
            binding.sidePanel.requestLayout()

            println("StreamingActivity: Configurações do sidePanel aplicadas com fallback padding: $fallbackPadding")
        }
    }

    private fun loadDefaultFreeChannels() {
        println("StreamingActivity: === CARREGANDO 5 CANAIS GRATUITOS ===")
        showLoadingState()

        binding.root.postDelayed({
            try {
                channelsList.clear()
                filteredChannelsList.clear()
                channelsList.addAll(DEFAULT_FREE_CHANNELS)
                filteredChannelsList.addAll(DEFAULT_FREE_CHANNELS)

                channelsAdapter = ChannelsAdapter(filteredChannelsList) { channel -> selectChannel(channel) }
                binding.recyclerChannels.adapter = channelsAdapter

                if (channelsList.isNotEmpty()) {
                    val firstChannel = channelsList.first()
                    currentChannel = firstChannel
                    updateChannelInfo()

                    // CARREGAR O PRIMEIRO CANAL AUTOMATICAMENTE
                    loadChannel(firstChannel.url)

                    Toast.makeText(this, "📺 ${channelsList.size} canais gratuitos carregados! Iniciando ${firstChannel.name}...", Toast.LENGTH_LONG).show()
                }

                binding.progressBar.visibility = View.GONE
                switchToChannelsTab()

                println("StreamingActivity: === CANAIS CARREGADOS COM SUCESSO ===")

            } catch (e: Exception) {
                println("StreamingActivity: ERRO ao carregar canais: ${e.message}")
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "❌ Erro ao carregar"
                Toast.makeText(this, "❌ Erro: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, 1000)
    }

    private fun selectChannel(channel: Channel) {
        println("StreamingActivity: === CANAL SELECIONADO: ${channel.name} ===")
        currentChannel = channel
        updateChannelInfo()
        loadChannel(channel.url)
        switchToInfoTab()
    }

    private fun loadChannel(url: String) {
        println("StreamingActivity: === CARREGANDO CANAL: $url ===")
        showLoadingState()

        if (!isValidStreamUrl(url)) {
            binding.progressBar.visibility = View.GONE
            binding.tvStatus.text = "❌ URL inválida"
            Toast.makeText(this, "❌ URL inválida: $url", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // Parar reprodução atual
            exoPlayer?.stop()
            exoPlayer?.clearMediaItems()

            // Criar DataSource factory
            val dataSourceFactory = DefaultDataSource.Factory(this)

            // Criar MediaItem
            val mediaItem = MediaItem.fromUri(Uri.parse(url))

            // Criar HLS MediaSource
            val hlsMediaSourceFactory = HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
            val mediaSource = hlsMediaSourceFactory.createMediaSource(mediaItem)

            // Configurar e iniciar player
            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true

            println("StreamingActivity: Player configurado para: $url")

        } catch (e: Exception) {
            println("StreamingActivity: ERRO ao configurar player: ${e.message}")
            e.printStackTrace()

            binding.progressBar.visibility = View.GONE
            binding.tvStatus.text = "❌ Erro de configuração"
            Toast.makeText(this, "❌ Erro ao carregar canal: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun isValidStreamUrl(url: String): Boolean {
        val isValid = url.isNotEmpty() &&
                (url.startsWith("http://") || url.startsWith("https://")) &&
                url.length > 10 &&
                (url.contains(".m3u8") || url.contains("hls") || url.contains("playlist"))

        println("StreamingActivity: URL $url é válida: $isValid")
        return isValid
    }

    private fun tryNextValidChannel() {
        val currentIndex = channelsList.indexOfFirst { it.id == currentChannel?.id }
        println("StreamingActivity: Tentando próximo canal. Índice atual: $currentIndex")

        for (i in (currentIndex + 1) until channelsList.size) {
            val nextChannel = channelsList[i]
            if (isValidStreamUrl(nextChannel.url)) {
                println("StreamingActivity: Próximo canal válido encontrado: ${nextChannel.name}")
                selectChannel(nextChannel)
                return
            }
        }

        // Se chegou ao fim, voltar para o primeiro
        if (channelsList.isNotEmpty()) {
            println("StreamingActivity: Voltando ao primeiro canal")
            selectChannel(channelsList.first())
        } else {
            binding.progressBar.visibility = View.GONE
            binding.tvStatus.text = "⚠️ Nenhum canal disponível"
            Toast.makeText(this, "📺 Nenhum canal disponível", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        binding.tvAppName.text = "Cinema Elite Player"

        binding.playerContainer.setOnClickListener {
            if (isControlsVisible) hideControls() else showControls()
        }

        binding.btnPlayPause.setOnClickListener {
            exoPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    Toast.makeText(this, "⏸️ Pausado", Toast.LENGTH_SHORT).show()
                } else {
                    player.play()
                    Toast.makeText(this, "▶️ Reproduzindo", Toast.LENGTH_SHORT).show()
                }
            }
            scheduleHideControls()
        }

        binding.btnMute.setOnClickListener {
            exoPlayer?.let { player ->
                player.volume = if (player.volume > 0f) {
                    binding.btnMute.setImageResource(R.drawable.ic_volume_off_modern)
                    Toast.makeText(this, "🔇 Sem som", Toast.LENGTH_SHORT).show()
                    0f
                } else {
                    binding.btnMute.setImageResource(R.drawable.ic_volume_up)
                    Toast.makeText(this, "🔊 Com som", Toast.LENGTH_SHORT).show()
                    1f
                }
            }
            scheduleHideControls()
        }

        binding.btnFullScreen.setOnClickListener {
            toggleFullScreen()
            scheduleHideControls()
        }

        binding.btnSkipChannel.setOnClickListener {
            Toast.makeText(this, "⏭️ Próximo canal...", Toast.LENGTH_SHORT).show()
            tryNextValidChannel()
            scheduleHideControls()
        }

        binding.tabChannels.setOnClickListener { switchToChannelsTab() }
        binding.tabInfo.setOnClickListener { switchToInfoTab() }

        // ========== CONFIGURAR CLIQUE NA ENGRENAGEM ==========
        binding.btnAdminAccess.setOnClickListener {
            println("StreamingActivity: 🔧 Clique na engrenagem detectado!")
            Toast.makeText(this, "⚙️ Abrindo configurações...", Toast.LENGTH_SHORT).show()
            openAdminSettings()
        }
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "🔄 Carregando..."
    }

    private fun updateChannelInfo() {
        currentChannel?.let { channel ->
            binding.tvChannelName.text = channel.name
            binding.tvChannelDescription.text = channel.description
        }
    }

    private fun switchToChannelsTab() {
        if (!isChannelsTabActive) {
            isChannelsTabActive = true
            binding.tabChannels.setBackgroundColor(getColor(R.color.primary_color))
            binding.tabChannels.setTextColor(getColor(R.color.white))
            binding.tabInfo.setBackgroundColor(getColor(R.color.surface_dark))
            binding.tabInfo.setTextColor(getColor(R.color.text_secondary))
            binding.channelsSection.visibility = View.VISIBLE
            binding.infoPanel.visibility = View.GONE
        }
    }

    private fun switchToInfoTab() {
        if (isChannelsTabActive) {
            isChannelsTabActive = false
            binding.tabInfo.setBackgroundColor(getColor(R.color.primary_color))
            binding.tabInfo.setTextColor(getColor(R.color.white))
            binding.tabChannels.setBackgroundColor(getColor(R.color.surface_dark))
            binding.tabChannels.setTextColor(getColor(R.color.text_secondary))
            binding.channelsSection.visibility = View.GONE
            binding.infoPanel.visibility = View.VISIBLE
        }
    }

    private fun showControls() {
        isControlsVisible = true
        binding.playerOverlay.visibility = View.VISIBLE
        binding.playerOverlay.animate().alpha(1f).setDuration(300).setListener(null)
        scheduleHideControls()
    }

    private fun hideControls() {
        isControlsVisible = false
        binding.playerOverlay.animate().alpha(0f).setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.playerOverlay.visibility = View.GONE
                }
            })
    }

    private fun scheduleHideControls() {
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
        hideControlsHandler.postDelayed(hideControlsRunnable, 3000)
    }

    private fun toggleFullScreen() {
        if (isFullScreen) exitFullScreen() else enterFullScreen()
        isFullScreen = !isFullScreen
    }

    private fun enterFullScreen() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        binding.sidePanel.visibility = View.GONE

        val layoutParams = binding.playerContainer.layoutParams as LinearLayout.LayoutParams
        layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        layoutParams.weight = 100f // Ocupa toda a tela
        binding.playerContainer.layoutParams = layoutParams
        binding.btnFullScreen.setImageResource(R.drawable.ic_fullscreen_exit)
    }

    private fun exitFullScreen() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        binding.sidePanel.visibility = View.VISIBLE

        val layoutParams = binding.playerContainer.layoutParams as LinearLayout.LayoutParams
        layoutParams.width = 0
        layoutParams.weight = 60f // Volta para 60%
        binding.playerContainer.layoutParams = layoutParams
        binding.btnFullScreen.setImageResource(R.drawable.ic_fullscreen)

        // REAPLICAR CONFIGURAÇÕES CUSTOMIZADAS APÓS SAIR DO FULLSCREEN - INLINE
        println("StreamingActivity: Saindo do fullscreen - reaplicando configurações")

        // Configuração do player inline
        binding.playerContainer.post {
            binding.playerContainer.setPadding(0, 0, 0, 0)
            val playerParams = binding.playerContainer.layoutParams as LinearLayout.LayoutParams
            playerParams.setMargins(0, 0, 0, 0)
            binding.playerContainer.layoutParams = playerParams
            binding.playerView.setPadding(0, 0, 0, 0)
            binding.playerContainer.requestLayout()
            println("StreamingActivity: Configurações do player reaplicadas")
        }

        // Configuração da playlist
        applySidePanelConfiguration()
    }

    // ========== LIFECYCLE METHODS ==========

    override fun onStart() {
        super.onStart()
        println("StreamingActivity: onStart() - Player já inicializado")
    }

    override fun onResume() {
        super.onResume()
        println("StreamingActivity: onResume() - Retomando reprodução")
        exoPlayer?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        println("StreamingActivity: onPause() - Pausando reprodução")
        exoPlayer?.playWhenReady = false
    }

    override fun onStop() {
        super.onStop()
        println("StreamingActivity: onStop() - Parando player")
        exoPlayer?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        println("StreamingActivity: === DESTRUINDO STREAMING ACTIVITY ===")

        // Desregistrar LOCAL broadcast receiver (sempre seguro)
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(configChangeReceiver)
            println("StreamingActivity: ✅ Local broadcast receiver desregistrado")
        } catch (e: Exception) {
            println("StreamingActivity: ⚠️ Erro ao desregistrar local receiver: ${e.message}")
        }

        // Desregistrar GLOBAL broadcast receiver (se foi registrado)
        try {
            unregisterReceiver(globalReceiver)
            println("StreamingActivity: ✅ Global broadcast receiver desregistrado")
        } catch (e: IllegalArgumentException) {
            // Receiver não estava registrado - normal se houve erro na criação
            println("StreamingActivity: ℹ️ Global receiver não estava registrado")
        } catch (e: Exception) {
            println("StreamingActivity: ⚠️ Erro ao desregistrar global receiver: ${e.message}")
        }

        // Limpar handlers e resources
        hideControlsHandler.removeCallbacks(hideControlsRunnable)

        // Liberar ExoPlayer
        exoPlayer?.release()
        exoPlayer = null

        println("StreamingActivity: ✅ Recursos liberados")
    }

    override fun onBackPressed() {
        if (isFullScreen) {
            toggleFullScreen()
        } else {
            super.onBackPressed()
        }
    }
}