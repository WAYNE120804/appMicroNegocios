package com.sebas.tiendaropa.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sebas.tiendaropa.data.prefs.SettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudTopBar(state: SettingsState) {
    TopAppBar(
        title = { Text(state.storeName, style = MaterialTheme.typography.titleLarge) },
        actions = {
            // Logo más grande (40dp) y redondeado
            state.logoUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Logo",
                    modifier = Modifier
                        .padding(end = 12.dp) // separa del borde derecho
                        .size(40.dp)
                        .clip(CircleShape)
                )
            }
            // --> Ya NO hay botón de Configuración aquí
        }
    )
}

