package com.yatrain.app.ui.screens.saved

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yatrain.app.ui.theme.AppNavy

@Composable
fun SavedTrainsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Saved Trains",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = AppNavy,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Your saved trains will appear here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
