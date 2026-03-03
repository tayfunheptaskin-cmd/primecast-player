package com.example.primecastplayer

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.primecastplayer.iptv.CachedIptvRepository
import com.example.primecastplayer.iptv.IptvCategory
import com.example.primecastplayer.iptv.IptvChannel
import com.example.primecastplayer.iptv.IptvRepository
import com.example.primecastplayer.iptv.PlaylistSource
import com.example.primecastplayer.iptv.SamplePlaylistRepository
import com.example.primecastplayer.ui.theme.PrimeCastTheme
import kotlinx.coroutines.launch

private val AppPanel = Color(0xFF0D1330)
private val CategoryOptions = listOf(IptvCategory.LIVE_TV, IptvCategory.MOVIES, IptvCategory.SERIES)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrimeCastTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppPanel
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
    val context = LocalContext.current
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
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
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
        containerColor = AppPanel,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(AppPanel)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            HeaderBar(onPlaylistClick = { showPlaylistDialog = true })
            Spacer(modifier = Modifier.height(8.dp))

            HeroCategoryRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )
            Spacer(modifier = Modifier.height(8.dp))

            PlaylistStatusRow(
                source = playlistSource,
                playlistUrl = playlistUrl,
                isLoading = isLoading
            )
            Spacer(modifier = Modifier.height(7.dp))

            ChannelQuickRow(
                channels = channels,
                selectedChannelId = selectedChannelId,
                onChannelSelected = { selectedChannelId = it }
            )
            Spacer(modifier = Modifier.height(7.dp))

            SelectedChannelInfo(selectedChannel = selectedChannel)
            Spacer(modifier = Modifier.height(9.dp))

            QuickActionsBar(
                onActionClick = { action ->
                    when (action) {
                        "Manage Playlists" -> showPlaylistDialog = true
                        "Refresh" -> scope.launch { loadPlaylist(forceRefresh = true) }
                        "Exit" -> showExitDialog = true
                        else -> scope.launch {
                            snackbarHostState.showSnackbar("$action ozelligi yakinda eklenecek.")
                        }
                    }
                }
            )
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
                    scope.launch { loadPlaylist(forceRefresh = true, overrideUrl = newUrl) }
                }
            }
        )
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Cikis") },
            text = { Text("Uygulamadan cikmak istiyor musun?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    }
                ) { Text("Evet") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Hayir") }
            }
        )
    }
}

@Composable
private fun HeaderBar(onPlaylistClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
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
                    color = Color(0xFF9EAAD6),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onPlaylistClick) {
                Text("Playlist URL", color = Color(0xFFA7C2FF))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(46.dp)
                .background(
                    brush = Brush.verticalGradient(listOf(Color(0xFF4EA2FF), Color(0xFF3370F4))),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "PLAY",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun HeroCategoryRow(
    selectedCategory: IptvCategory,
    onCategorySelected: (IptvCategory) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val cardWidth = (maxWidth - (gap * 2)) / 3
        val cardHeight = if (maxHeight < 220.dp) 82.dp else 96.dp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap)
        ) {
            CategoryOptions.forEach { category ->
                HeroCategoryCard(
                    category = category,
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    modifier = Modifier
                        .width(cardWidth)
                        .height(cardHeight)
                )
            }
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
            listOf(Color(0xFF5D86F8), Color(0xFF456ED8))
        )

        IptvCategory.MOVIES -> Brush.verticalGradient(
            listOf(Color(0xFF7C63CE), Color(0xFF6046B1))
        )

        IptvCategory.SERIES -> Brush.verticalGradient(
            listOf(Color(0xFF4FB9B5), Color(0xFF3B9B98))
        )
    }

    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.titleText(),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlaylistStatusRow(
    source: PlaylistSource,
    playlistUrl: String,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2344))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kaynak: ${source.asLabel()}",
                color = Color(0xFFD7E4FF),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = playlistUrl,
                color = Color(0xFFA7B8E7),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(16.dp)
                        .padding(start = 8.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun ChannelQuickRow(
    channels: List<IptvChannel>,
    selectedChannelId: String?,
    onChannelSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121A35))
    ) {
        if (channels.isEmpty()) {
            Text(
                text = "Bu kategoride kanal bulunamadi.",
                color = Color(0xFFBBC7EC),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp)
            )
        } else {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                channels.take(12).forEach { channel ->
                    val selected = selectedChannelId == channel.id
                    Card(
                        modifier = Modifier
                            .width(170.dp)
                            .clickable { onChannelSelected(channel.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) Color(0xFF4D536E) else Color(0xFF303751)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = channel.name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = channel.groupTitle.ifEmpty { "Genel" },
                                color = Color(0xFFB8C6EF),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedChannelInfo(selectedChannel: IptvChannel?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151C38))
    ) {
        if (selectedChannel == null) {
            Text(
                text = "Detaylar icin kanal secin.",
                color = Color(0xFFB8C6EF),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(10.dp)
            )
        } else {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = selectedChannel.name,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${selectedChannel.category.displayName} | ${selectedChannel.groupTitle.ifEmpty { "Genel" }}",
                    color = Color(0xFFB8C6EF),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickActionsBar(onActionClick: (String) -> Unit) {
    val actions = remember {
        listOf(
            QuickAction("Account", "Hesap", Color(0xFFC14A96)),
            QuickAction("Manage Playlists", "Playlist", Color(0xFFC14A96)),
            QuickAction("Refresh", "Refresh", Color(0xFFC14A96)),
            QuickAction("Language", "Language", Color(0xFFC14A96)),
            QuickAction("Settings", "Settings", Color(0xFFC14A96)),
            QuickAction("Exit", "Exit", Color(0xFFB4000F))
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val gap = 8.dp
        val ideal = (maxWidth - (gap * 5)) / 6
        val compact = ideal < 88.dp

        if (compact) {
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                actions.forEach { item ->
                    ActionButton(
                        action = item,
                        onClick = { onActionClick(item.id) },
                        modifier = Modifier.width(106.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                actions.forEach { item ->
                    ActionButton(
                        action = item,
                        onClick = { onActionClick(item.id) },
                        modifier = Modifier.width(ideal)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = action.color),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        modifier = modifier.height(42.dp)
    ) {
        Text(
            text = action.label,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
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
            TextButton(onClick = { onConfirm(draftUrl.trim()) }) { Text("Yukle") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Iptal") }
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
