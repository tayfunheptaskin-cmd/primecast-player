package com.example.primecastplayer

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.primecastplayer.iptv.IptvCategory
import com.example.primecastplayer.iptv.IptvChannel
import com.example.primecastplayer.iptv.IptvRepository
import com.example.primecastplayer.iptv.SamplePlaylistRepository
import com.example.primecastplayer.ui.theme.PrimeCastTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PrimeCastTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PrimeCastPlayerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrimeCastPlayerApp(
    repository: IptvRepository = SamplePlaylistRepository
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedCategory by rememberSaveable { mutableStateOf(IptvCategory.LIVE_TV) }
    val channels = remember(selectedCategory, repository) { repository.channelsFor(selectedCategory) }
    var selectedChannelId by rememberSaveable(selectedCategory) { mutableStateOf<String?>(null) }

    LaunchedEffect(channels) {
        if (selectedChannelId == null || channels.none { it.id == selectedChannelId }) {
            selectedChannelId = channels.firstOrNull()?.id
        }
    }

    val selectedChannel = channels.firstOrNull { it.id == selectedChannelId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PrimeCast IPTV") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val wideLayout = maxWidth >= 900.dp

            if (wideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.42f)
                            .fillMaxHeight()
                    ) {
                        CategoryTabs(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )
                        ChannelList(
                            channels = channels,
                            selectedChannelId = selectedChannelId,
                            onChannelSelected = { selectedChannelId = it },
                            modifier = Modifier.weight(1f)
                        )
                        MenuButtons(
                            onActionClick = { action ->
                                when (action) {
                                    "Refresh" -> scope.launch {
                                        snackbarHostState.showSnackbar("Playlist yenilendi.")
                                    }

                                    "Exit" -> (context as? Activity)?.finish()
                                    else -> scope.launch {
                                        snackbarHostState.showSnackbar("$action ozelligi yakinda eklenecek.")
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                    ) {
                        ChannelDetails(selectedChannel = selectedChannel)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    CategoryTabs(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { selectedCategory = it }
                    )
                    ChannelList(
                        channels = channels,
                        selectedChannelId = selectedChannelId,
                        onChannelSelected = { selectedChannelId = it },
                        modifier = Modifier.weight(1f)
                    )
                    ChannelDetails(selectedChannel = selectedChannel)
                    MenuButtons(
                        onActionClick = { action ->
                            when (action) {
                                "Refresh" -> scope.launch {
                                    snackbarHostState.showSnackbar("Playlist yenilendi.")
                                }

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
}

@Composable
private fun CategoryTabs(
    selectedCategory: IptvCategory,
    onCategorySelected: (IptvCategory) -> Unit
) {
    val categories = IptvCategory.entries
    TabRow(selectedTabIndex = categories.indexOf(selectedCategory)) {
        categories.forEach { category ->
            Tab(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                text = { Text(category.displayName) }
            )
        }
    }
}

@Composable
private fun ChannelList(
    channels: List<IptvChannel>,
    selectedChannelId: String?,
    onChannelSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (channels.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize()
        ) {
            Text(
                text = "Bu kategoride kanal bulunamadi.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(channels, key = { it.id }) { channel ->
            val selected = channel.id == selectedChannelId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onChannelSelected(channel.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channel.groupTitle.ifEmpty { "Genel" },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelDetails(selectedChannel: IptvChannel?) {
    if (selectedChannel == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Text(
                text = "Detaylari gormek icin bir kanal secin.",
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = selectedChannel.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kategori: ${selectedChannel.category.displayName}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Grup: ${selectedChannel.groupTitle.ifEmpty { "Genel" }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Akis URL",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = selectedChannel.streamUrl,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            StreamPreview(
                streamUrl = selectedChannel.streamUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
private fun StreamPreview(
    streamUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
        modifier = modifier.widthIn(min = 200.dp),
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
private fun MenuButtons(onActionClick: (String) -> Unit) {
    val actions = listOf("Account", "Manage Playlists", "Refresh", "Language", "Settings", "Exit")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            Button(
                onClick = { onActionClick(action) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(action)
            }
        }
    }
}