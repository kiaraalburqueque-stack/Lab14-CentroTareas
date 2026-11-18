package com.alburquequearzapalo.centrotareas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alburquequearzapalo.centrotareas.model.CargaUi
import com.alburquequearzapalo.centrotareas.model.PrimosUi
import com.alburquequearzapalo.centrotareas.model.RegistroTiempo
import com.alburquequearzapalo.centrotareas.ui.theme.VerdeExito
import com.alburquequearzapalo.centrotareas.ui.theme.VerdeFondo
import com.alburquequearzapalo.centrotareas.viewmodel.TareasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaTareas(vm: TareasViewModel) {
    val mensaje by vm.mensaje.collectAsState()
    val progreso by vm.progreso.collectAsState()
    val corriendo by vm.corriendo.collectAsState()
    val primos by vm.primos.collectAsState()
    val carga by vm.carga.collectAsState()
    val simularError by vm.simularError.collectAsState()
    val limiteTiempo by vm.limiteTiempo.collectAsState()
    val intento by vm.intento.collectAsState()
    val historial by vm.historial.collectAsState()
    // Ajusta este valor si en tu emulador tarda menos de 2 s o más de 5 s
    var textoN by rememberSaveable { mutableStateOf("2000000") }
    // Prueba de vida: si este contador deja de sumar, la pantalla está congelada
    var toques by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CentroTareas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Tarjeta("Tarea con progreso") {
                Button(onClick = { toques++ }) { Text("Tócame: $toques") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.tareaLarga() },
                        enabled = !corriendo
                    ) { Text("Iniciar") }
                    OutlinedButton(
                        onClick = { vm.cancelar() },
                        enabled = corriendo
                    ) { Text("Cancelar") }
                }
                LinearProgressIndicator(
                    progress = { progreso },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${(progreso * 100).toInt()} %")
                Text(mensaje)
            }
            Tarjeta("Contar números primos") {
                val calculando = primos is PrimosUi.Calculando
                OutlinedTextField(
                    value = textoN,
                    onValueChange = { textoN = it },
                    label = { Text("Contar primos hasta N") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            // Ignora textos no numéricos o menores que 2
                            val n = textoN.trim().toIntOrNull()
                            if (n != null && n >= 2) vm.contar(n)
                        },
                        enabled = !calculando
                    ) { Text("Contar") }
                    OutlinedButton(
                        onClick = { vm.cancelarPrimos() },
                        enabled = calculando
                    ) { Text("Cancelar") }
                }
                when (val estado = primos) {
                    PrimosUi.Inicial -> Text("Escribe N y pulsa Contar")
                    PrimosUi.Calculando -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Text("Calculando...")
                    }
                    is PrimosUi.Listo -> Recuadro(
                        texto = "Primos encontrados: ${estado.cantidad}\nTiempo: ${estado.milisegundos} ms",
                        fondo = VerdeFondo,
                        colorTexto = VerdeExito
                    )
                    PrimosUi.Cancelado -> Text("Cancelado")
                }
            }
            Tarjeta("Dos cargas") {
                val cargando = carga is CargaUi.Cargando
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = simularError,
                        onCheckedChange = { vm.cambiarSimularError(it) }
                    )
                    Text("Simular error")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = limiteTiempo,
                        onCheckedChange = { vm.cambiarLimiteTiempo(it) }
                    )
                    Text("Tiempo máximo 2 s")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { vm.cargarUnaPorUna() },
                        enabled = !cargando
                    ) { Text("Una por una") }
                    Button(
                        onClick = { vm.cargarALaVez() },
                        enabled = !cargando
                    ) { Text("A la vez") }
                }
                when (val estado = carga) {
                    CargaUi.Inicial -> Text("Elige cómo cargar el perfil y los pedidos")
                    CargaUi.Cargando -> Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        Text(
                            if (intento > 1) "Reintentando... (intento $intento de ${TareasViewModel.MAX_INTENTOS})"
                            else "Cargando..."
                        )
                    }
                    is CargaUi.Exito -> Recuadro(
                        texto = "Modo: ${estado.modo}\nPerfil: ${estado.perfil}\n" +
                            "Pedidos: ${estado.pedidos}\nTiempo: ${estado.milisegundos} ms",
                        fondo = VerdeFondo,
                        colorTexto = VerdeExito
                    )
                    is CargaUi.Error -> {
                        Recuadro(
                            texto = estado.mensaje,
                            fondo = MaterialTheme.colorScheme.errorContainer,
                            colorTexto = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(onClick = { vm.reintentar() }) { Text("Reintentar") }
                    }
                }
                if (historial.isNotEmpty()) {
                    TablaTiempos(historial)
                    TextButton(onClick = { vm.limpiarHistorial() }) { Text("Limpiar tabla") }
                }
            }
        }
    }
}

@Composable
fun Tarjeta(titulo: String, contenido: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = titulo,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            contenido()
        }
    }
}

@Composable
fun Recuadro(texto: String, fondo: Color, colorTexto: Color) {
    Text(
        text = texto,
        color = colorTexto,
        modifier = Modifier
            .fillMaxWidth()
            .background(fondo, RoundedCornerShape(8.dp))
            .padding(12.dp)
    )
}

// Mejora IA 4: tabla que acumula los tiempos de cada modo
@Composable
fun TablaTiempos(historial: List<RegistroTiempo>) {
    HorizontalDivider()
    Text(
        "Comparación de tiempos",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold
    )
    FilaTabla("Modo", "Veces", "Promedio", "Último", negrita = true)
    listOf("Una por una", "A la vez").forEach { modo ->
        val tiempos = historial.filter { it.modo == modo }.map { it.milisegundos }
        if (tiempos.isNotEmpty()) {
            FilaTabla(
                modo,
                "${tiempos.size}",
                "${tiempos.average().toLong()} ms",
                "${tiempos.last()} ms"
            )
        }
    }
}

@Composable
private fun FilaTabla(a: String, b: String, c: String, d: String, negrita: Boolean = false) {
    val peso = if (negrita) FontWeight.Bold else FontWeight.Normal
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(a, modifier = Modifier.weight(1.4f), fontWeight = peso)
        Text(b, modifier = Modifier.weight(0.8f), fontWeight = peso)
        Text(c, modifier = Modifier.weight(1.2f), fontWeight = peso)
        Text(d, modifier = Modifier.weight(1.1f), fontWeight = peso)
    }
}
