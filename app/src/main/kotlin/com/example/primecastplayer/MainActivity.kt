package com.example.primecastplayer

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.primecastplayer.iptv.CachedIptvRepository
import com.example.primecastplayer.iptv.IptvCategory
import com.example.primecastplayer.iptv.IptvChannel
import com.example.primecastplayer.iptv.IptvRepository
import com.example.primecastplayer.iptv.PlaylistSource
import com.example.primecastplayer.iptv.SamplePlaylistRepository
import com.example.primecastplayer.ui.theme.PrimeCastTheme
import kotlinx.coroutines.launch

private val AppCanvas = Color(0xFFD8D9E2)
private val AppPanel = Color(0xFF0C1222)
private val CategoryOptions = listOf(
    IptvCategory.LIVE_TV,
    IptvCategory.MOVIES,
    IptvCategory.SERIES
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrimeCastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppCanvas
                ) {
                    PrimeCastPlayerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimeCastPlayerApp(repository: IptvRepository? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appContext = context.applicationContext
    val resolvedRepository = remember(repository, appContext) {
        repository ?: CachedIptvRepository(appContext)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedCategory by rememberSaveable { mutableStateOf(IptvCategory.LIVE_TV) }
    var allChannels by remember { mutableStateOf(SamplePlaylistRepository.channels) }
    var selectedChannelId by rememberSaveable(selectedCategory) { mutableStateOf<String?>(null) }
    var playlistSource by remember { mutableStateOf(PlaylistSource.SAMPLE) }
    var isLoading by remember { mutableStateOf(false) }
    var showPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var playlistUrl by rememberSaveable {
        mutableStateOf(
            resolvedRepository.cachedPlaylistUrl() ?: CachedIptvRepository.DEFAULT_PLAYLIST_URL
        )
    }

    val channels = remember(allChannels, selectedCategory) {
        allChannels.filter { it.category == selectedCategory }
    }

    LaunchedEffect(channels) {
        if (selectedChannelId == null || channels.none { it.id == selectedChannelId }) {
            selectedChannelId = channels.firstOrNull()?.id
        }
    }

    suspend fun loadPlaylist(forceRefresh: Boolean, overrideUrl: String? = null) {
        isLoading = true
        try {
            val requestedUrl = (overrideUrl ?: playlistUrl).trim()
            val result = resolvedRepository.loadPlaylist(requestedUrl, forceRefresh)
            allChannels = result.channels
            playlistSource = result.source

            if (result.usedUrl.startsWith("http", ignoreCase = true)) {
                playlistUrl = result.usedUrl
            }

            selectedCategory = resolveCategory(selectedCategory, result.channels)
            result.message?.let { snackbarHostState.showSnackbar(it) }
        } catch (error: Exception) {
            snackbarHostState.showSnackbar("Playlist yuklenemedi: ${error.message}")
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(resolvedRepository) {
        loadPlaylist(forceRefresh = false)
    }

    val selectedChannel = channels.firstOrNull { it.id == selectedChannelId }

    Scaffold(
        containerColor = AppCanvas,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 900.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = AppPanel)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    HeaderRow(onPlaylistClick = { showPlaylistDialog = true })
                    Spacer(modifier = Modifier.height(10.dp))

                    HeroCategoryRow(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    PlaylistStatusStrip(
                        source = playlistSource,
                        playlistUrl = playlistUrl,
                        isLoading = isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ChannelListPanel(
                        channels = channels,
                        selectedChannelId = selectedChannelId,
                        onChannelSelected = { selectedChannelId = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SelectedChannelPanel(selectedChannel = selectedChannel)
                    Spacer(modifier = Modifier.height(10.dp))

                    QuickActionsBar(
                        onActionClick = { action ->
                            when (action) {
                                "Manage Playlists" -> showPlaylistDialog = true
                                "Refresh" -> scope.launch { loadPlaylist(forceRefresh = true) }
                                "Exit" -> (context as? Activity)?.finish()
                                else -> scope.launch {
                                    snackbarHostState.showSnackbar("$action ozelligi yakinda eklenecek.")
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showPlaylistDialog) {
        PlaylistUrlDialog(
            initialUrl = playlistUrl,
            onDismiss = { showPlaylistDialog = false },
            onConfirm = { newUrl ->
                showPlaylistDialog = false
                if (newUrl.isBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("Gecerli bir playlist URL girin.") }
                } else {
                    playlistUrl = newUrl
                    scope.launch {
                        loadPlaylist(forceRefresh = true, overrideUrl = newUrl)
                    }
                }
            }
        )
    }
}

@Composable
private fun HeaderRow(onPlaylistClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "PrimeCast IPTV",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Player",
                color = Color(0xFFA4AED1),
                style = MaterialTheme.typography.bodySmall
            )
        }
        TextButton(onClick = onPlaylistClick) {
            Text(text = "Playlist URL", color = Color(0xFF9DC2FF))
        }
    }
}

@Composable
private fun HeroCategoryRow(
    selectedCategory: IptvCategory,
    onCategorySelected: (IptvCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CategoryOptions.forEach { category ->
            HeroCategoryCard(
                category = category,
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                modifier = Modifier
                    .weight(1f)
                    .height(118.dp)
            )
        }
    }
}

@Composable
private fun HeroCategoryCard(
    category: IptvCategory,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = when (category) {
        IptvCategory.LIVE_TV -> Brush.verticalGradient(
            listOf(Color(0xFF618DFF), Color(0xFF4069D0))
        )

        IptvCategory.MOVIES -> Brush.verticalGradient(
            listOf(Color(0xFF7F63D3), Color(0xFF5E43AD))
        )

        IptvCategory.SERIES -> Brush.verticalGradient(
            listOf(Color(0xFF5F73BE), Color(0xFF465A9E))
        )
    }

    val border = if (selected) BorderStroke(2.dp, Color(0xFFEAF0FF)) else null
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.titleText(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlaylistStatusStrip(
    source: PlaylistSource,
    playlistUrl: String,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2340))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kaynak: ${source.asLabel()}",
                color = Color(0xFFD5E0FF),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = playlistUrl,
                color = Color(0xFFA5B4DE),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .padding(start = 8.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun ChannelListPanel(
    channels: List<IptvChannel>,
    selectedChannelId: String?,
    onChannelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11182E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Channels",
                color = Color(0xFFEAF0FF),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (channels.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Bu kategoride kanal bulunamadi.",
                        color = Color(0xFFACB6D9)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(channels, key = { it.id }) { channel ->
                        val selected = selectedChannelId == channel.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { onChannelSelected(channel.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    Color(0xFF4A4E65)
                                } else {
                                    Color(0xFF2F3447)
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = channel.name,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = channel.groupTitle.ifEmpty { "Genel" },
                                    color = Color(0xFFC0C8E4),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedChannelPanel(selectedChannel: IptvChannel?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151C34))
    ) {
        if (selectedChannel == null) {
            Text(
                text = "Detaylari gormek icin bir kanal secin.",
                color = Color(0xFFC0C8E4),
                modifier = Modifier.padding(12.dp)
            )
            return
        }

        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = selectedChannel.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${selectedChannel.category.displayName} | ${selectedChannel.groupTitle.ifEmpty { "Genel" }}",
                color = Color(0xFFB4BFDE),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            StreamPreview(
                streamUrl = selectedChannel.streamUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 130.dp, max = 180.dp)
                    .background(Color.Black)
            )
        }
    }
}

@Composable
private fun StreamPreview(
    streamUrl: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val exoPlayer = remember(streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = false
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { playerContext ->
            PlayerView(playerContext).apply {
                useController = true
                player = exoPlayer
            }
        },
        update = { it.player = exoPlayer }
    )
}

@Composable
private fun QuickActionsBar(onActionClick: (String) -> Unit) {
    val actions = remember {
        listOf(
            QuickAction("Account", "Account", Color(0xFF4F70DA)),
            QuickAction("Manage Playlists", "Playlist", Color(0xFF8A4CC9)),
            QuickAction("Refresh", "Refresh", Color(0xFF6A4ED9)),
            QuickAction("Language", "Language", Color(0xFF8A4CC9)),
            QuickAction("Settings", "Settings", Color(0xFF6A4ED9)),
            QuickAction("Exit", "Exit", Color(0xFFC9303D))
        )
    }
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { item ->
            Button(
                onClick = { onActionClick(item.id) },
                colors = ButtonDefaults.buttonColors(containerColor = item.color),
                modifier = Modifier.width(118.dp)
            ) {
                Text(
                    text = item.label,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PlaylistUrlDialog(
    initialUrl: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var draftUrl by rememberSaveable(initialUrl) {
        mutableStateOf(
            initialUrl.takeIf { it.startsWith("http", ignoreCase = true) }
                ?: CachedIptvRepository.DEFAULT_PLAYLIST_URL
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playlist URL") },
        text = {
            OutlinedTextField(
                value = draftUrl,
                onValueChange = { draftUrl = it },
                singleLine = true,
                label = { Text("M3U URL") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draftUrl.trim()) }) {
                Text("Yukle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Iptal")
            }
        }
    )
}

private data class QuickAction(
    val id: String,
    val label: String,
    val color: Color
)

private fun IptvCategory.titleText(): String = when (this) {
    IptvCategory.LIVE_TV -> "LIVE TV"
    IptvCategory.MOVIES -> "MOVIES"
    IptvCategory.SERIES -> "SERIES"
}

private fun PlaylistSource.asLabel(): String = when (this) {
    PlaylistSource.NETWORK -> "Ag"
    PlaylistSource.CACHE -> "Cache"
    PlaylistSource.SAMPLE -> "Ornek"
}

private fun resolveCategory(
    currentCategory: IptvCategory,
    channels: List<IptvChannel>
): IptvCategory {
    if (channels.any { it.category == currentCategory }) {
        return currentCategory
    }
    return CategoryOptions.firstOrNull { category ->
        channels.any { it.category == category }
    } ?: IptvCategory.LIVE_TV
}
