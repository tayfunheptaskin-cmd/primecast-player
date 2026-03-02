package com.example.primecastplayer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun MainActivityContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "PrimeCast Player", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { /* TODO: Navigate to Live TV Screen */ }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6200EE))) {
                Text("LIVE TV")
            }
            Button(onClick = { /* TODO: Navigate to Movies Screen */ }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3700B3))) {
                Text("MOVIES")
            }
            Button(onClick = { /* TODO: Navigate to Series Screen */ }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF03DAC5))) {
                Text("SERIES")
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
        Text(text = "Menu", style = MaterialTheme.typography.headlineMedium)
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val menuItems = listOf("Account", "Manage Playlists", "Refresh", "Language", "Settings", "Exit")
            menuItems.forEach { item ->
                Button(onClick = { /* TODO: Handle $item click */ }, modifier = Modifier.fillMaxWidth()) {
                    Text(item)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainActivityContent()
}