package com.alburquequearzapalo.centrotareas.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Esquema = lightColorScheme(
    primary = Azul,
    onPrimary = Color.White,
    primaryContainer = AzulClaro
)

@Composable
fun CentroTareasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Esquema,
        typography = Tipografia,
        content = content
    )
}
