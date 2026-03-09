package com.example.primecastplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val ScreenBackground = Color(0xFF050A24)
private val PanelBackground = Color(0xFF0D1336)
private val Accent = Color(0xFF2B5BFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainActivityContent()
            }
        }
    }
}

@Composable
fun MainActivityContent() {
    val countries = listOf(
        "AL | ALBANIA",
        "AM | ARMENIA",
        "AZ | AZERBAIJAN",
        "BE | BELGIUM",
        "BG | BULGARIA",
        "CH | SWITZERLAND",
        "DE | BUNDESLIGA",
        "DE | DAZN"
    )
    val channels = listOf(
        "AT | ATV 1 HD",
        "AT | ATV 2 HD",
        "AT | PULS 4 HD",
        "AT | ORF 1 HD",
        "AT | ORF 2 HD",
        "AT | ORF 3 HD",
        "AT | ORF SPORT HD",
        "AT | SRF 1 HD"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(12.dp)
    ) {
        HeaderBar()
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, Accent, RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChannelsPanel(
                countries = countries,
                channels = channels,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.34f)
                    .widthIn(min = 220.dp, max = 380.dp)
            )
            VideoPanel(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.66f)
            )
        }
    }
}

@Composable
private fun HeaderBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBackground, RoundedCornerShape(10.dp))
            .border(1.dp, Accent, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Anasayfa", color = Color.White)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Canli", color = Color(0xFFFFDF6E))
        Spacer(modifier = Modifier.width(12.dp))
        Text("Filmler", color = Color(0xFFB7C5FF))
        Spacer(modifier = Modifier.width(12.dp))
        Text("Diziler", color = Color(0xFFB7C5FF))
        Spacer(modifier = Modifier.weight(1f))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .wrapContentHeight()
                .widthIn(min = 180.dp, max = 260.dp),
            singleLine = true,
            label = { Text("Ara") }
        )
    }
}

@Composable
private fun ChannelsPanel(countries: List<String>, channels: List<String>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(PanelBackground, RoundedCornerShape(10.dp))
            .border(1.dp, Accent, RoundedCornerShape(10.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ChannelList(items = countries, modifier = Modifier.weight(1f))
        ChannelList(items = channels, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ChannelList(items: List<String>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(items) { item ->
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF142A78))
            ) {
                Text(text = item, color = Color.White, maxLines = 1)
            }
        }
    }
}

@Composable
private fun VideoPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(PanelBackground, RoundedCornerShape(10.dp))
            .border(1.dp, Accent, RoundedCornerShape(10.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val isWideScreen = maxWidth / maxHeight > 16f / 9f
            val videoModifier = if (isWideScreen) {
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(16f / 9f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            }

            Box(
                modifier = videoModifier
                    .background(Color.Black, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF3454E3), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Player Alani", color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        Surface {
            MainActivityContent()
        }
    }
}