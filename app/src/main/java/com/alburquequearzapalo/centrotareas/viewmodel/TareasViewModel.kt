package com.alburquequearzapalo.centrotareas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alburquequearzapalo.centrotareas.data.TareasRepository
import com.alburquequearzapalo.centrotareas.model.CargaUi
import com.alburquequearzapalo.centrotareas.model.PrimosUi
import com.alburquequearzapalo.centrotareas.model.RegistroTiempo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TareasViewModel(private val repo: TareasRepository) : ViewModel() {

    // ----- Tarea con progreso (pasos 1 a 4)
    private val _mensaje = MutableStateFlow("Listo")
    val mensaje: StateFlow<String> = _mensaje.asStateFlow()

    // Paso 3: progreso de 0f a 1f. Solo se expone la versión de lectura.
    private val _progreso = MutableStateFlow(0f)
    val progreso: StateFlow<Float> = _progreso.asStateFlow()

    // Paso 4: guardamos el Job para poder cancelarlo
    private var trabajo: Job? = null
    private val _corriendo = MutableStateFlow(false)
    val corriendo: StateFlow<Boolean> = _corriendo.asStateFlow()

    // Pasos 2, 3 y 4: la tarea corre en segundo plano con una corrutina,
    // informa su avance y se puede cancelar. El hilo principal queda libre.
    fun tareaLarga() {
        if (_corriendo.value) return // evita lanzar dos tareas a la vez
        trabajo = viewModelScope.launch {
            _corriendo.value = true
            try {
                _progreso.value = 0f
                _mensaje.value = "Trabajando..."
                Log.d("HILO", "Antes: ${Thread.currentThread().name}")
                // Mejora IA 1: el ViewModel solo recolecta el Flow del Repository
                repo.progresoTarea().collect { valor ->
                    _progreso.value = valor
                }
                Log.d("HILO", "Despues: ${Thread.currentThread().name}")
                _mensaje.value = "Terminó"
            } finally {
                // Se ejecuta siempre: termine bien o sea cancelada
                _corriendo.value = false
            }
        }
    }

    fun cancelar() {
        trabajo?.cancel()
        _mensaje.value = "Cancelado" // la barra se queda donde estaba
    }

    // ----- Contar primos (paso 5)
    private val _primos = MutableStateFlow<PrimosUi>(PrimosUi.Inicial)
    val primos: StateFlow<PrimosUi> = _primos.asStateFlow()
    private var trabajoPrimos: Job? = null

    fun contar(hasta: Int) {
        if (_primos.value is PrimosUi.Calculando) return
        trabajoPrimos = viewModelScope.launch {
            _primos.value = PrimosUi.Calculando
            val inicio = System.currentTimeMillis()
            try {
                val cantidad = repo.contarPrimos(hasta)
                _primos.value = PrimosUi.Listo(cantidad, System.currentTimeMillis() - inicio)
            } catch (e: CancellationException) {
                _primos.value = PrimosUi.Cancelado
                throw e // se relanza para no romper la cancelación
            }
        }
    }

    fun cancelarPrimos() {
        trabajoPrimos?.cancel()
    }

    // ----- Dos cargas (pasos 6 y 7)
    private val _carga = MutableStateFlow<CargaUi>(CargaUi.Inicial)
    val carga: StateFlow<CargaUi> = _carga.asStateFlow()

    // Paso 7: interruptor para simular que los pedidos fallan
    private val _simularError = MutableStateFlow(false)
    val simularError: StateFlow<Boolean> = _simularError.asStateFlow()

    fun cambiarSimularError(valor: Boolean) {
        _simularError.value = valor
    }

    // Mejora IA 2: tiempo máximo. Con el interruptor activo, si la carga
    // tarda más de 2 s se corta con withTimeout y se muestra "Tardó demasiado".
    private val _limiteTiempo = MutableStateFlow(false)
    val limiteTiempo: StateFlow<Boolean> = _limiteTiempo.asStateFlow()

    fun cambiarLimiteTiempo(valor: Boolean) {
        _limiteTiempo.value = valor
    }

    // Mejora IA 3: reintento automático. Los pedidos se intentan hasta 3 veces,
    // esperando 500 ms y luego 1000 ms (espera creciente) antes de rendirse.
    private val _intento = MutableStateFlow(1)
    val intento: StateFlow<Int> = _intento.asStateFlow()

    private suspend fun <T> conReintentos(bloque: suspend () -> T): T {
        var espera = 500L
        repeat(MAX_INTENTOS - 1) { i ->
            _intento.value = i + 1
            try {
                return bloque()
            } catch (e: Exception) {
                if (e is CancellationException) throw e // nunca se reintenta una cancelación
                Log.d("REINTENTO", "Intento ${i + 1} falló: ${e.message}. Espero $espera ms")
            }
            delay(espera)
            espera *= 2
        }
        _intento.value = MAX_INTENTOS
        return bloque() // último intento: si falla, el error llega al catch de la carga
    }

    // Mejora IA 4: historial de tiempos para comparar Una por una contra A la vez
    private val _historial = MutableStateFlow<List<RegistroTiempo>>(emptyList())
    val historial: StateFlow<List<RegistroTiempo>> = _historial.asStateFlow()

    fun limpiarHistorial() {
        _historial.value = emptyList()
    }

    // Paso 6: una carga detrás de otra -> 2 s + 3 s ≈ 5 s
    fun cargarUnaPorUna() = ejecutarCarga("Una por una") {
        val perfil = repo.cargarPerfil()
        val pedidos = conReintentos { repo.cargarPedidos(_simularError.value) }
        Pair(perfil, pedidos)
    }

    // Paso 6: las dos cargas a la vez con async -> máximo(2 s, 3 s) ≈ 3 s
    fun cargarALaVez() = ejecutarCarga("A la vez") {
        coroutineScope {
            val a = async { repo.cargarPerfil() }
            val b = async { conReintentos { repo.cargarPedidos(_simularError.value) } }
            Pair(a.await(), b.await())
        }
    }

    // Paso 7: el botón Reintentar vuelve a ejecutar la carga A la vez
    fun reintentar() = cargarALaVez()

    // Lógica común de las dos cargas: estado, tiempo, límite y errores
    private fun ejecutarCarga(
        modo: String,
        cargar: suspend () -> Pair<String, List<String>>
    ) {
        if (_carga.value is CargaUi.Cargando) return
        viewModelScope.launch {
            _carga.value = CargaUi.Cargando
            _intento.value = 1
            val inicio = System.currentTimeMillis()
            try {
                val (perfil, pedidos) =
                    if (_limiteTiempo.value) withTimeout(LIMITE_MS) { cargar() } else cargar()
                val ms = System.currentTimeMillis() - inicio
                _carga.value = CargaUi.Exito(modo, perfil, pedidos.size, ms)
                _historial.value = _historial.value + RegistroTiempo(modo, ms)
            } catch (e: TimeoutCancellationException) {
                // Va antes que el catch general: TimeoutCancellationException
                // es una CancellationException y si no se relanzaría.
                _carga.value = CargaUi.Error("Tardó demasiado")
            } catch (e: Exception) {
                if (e is CancellationException) throw e // la cancelación no es un error
                _carga.value = CargaUi.Error("No se pudo cargar: " + e.message)
            }
        }
    }

    companion object {
        private const val LIMITE_MS = 2000L
        const val MAX_INTENTOS = 3
    }
}
