package com.alburquequearzapalo.centrotareas.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alburquequearzapalo.centrotareas.data.TareasRepository
import com.alburquequearzapalo.centrotareas.model.CargaUi
import com.alburquequearzapalo.centrotareas.model.PrimosUi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                for (i in 1..100) {
                    withContext(Dispatchers.Default) {
                        if (i == 1) Log.d("HILO", "Dentro: ${Thread.currentThread().name}")
                        delay(40)
                    }
                    _progreso.value = i / 100f
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

    // Paso 6: una carga detrás de otra -> 2 s + 3 s ≈ 5 s
    fun cargarUnaPorUna() {
        if (_carga.value is CargaUi.Cargando) return
        viewModelScope.launch {
            _carga.value = CargaUi.Cargando
            val inicio = System.currentTimeMillis()
            val perfil = repo.cargarPerfil()
            val pedidos = repo.cargarPedidos(false)
            _carga.value = CargaUi.Exito(
                "Una por una", perfil, pedidos.size, System.currentTimeMillis() - inicio
            )
        }
    }

    // Paso 6: las dos cargas a la vez con async -> máximo(2 s, 3 s) ≈ 3 s
    fun cargarALaVez() {
        if (_carga.value is CargaUi.Cargando) return
        viewModelScope.launch {
            _carga.value = CargaUi.Cargando
            val inicio = System.currentTimeMillis()
            val (perfil, pedidos) = coroutineScope {
                val a = async { repo.cargarPerfil() }
                val b = async { repo.cargarPedidos(false) }
                Pair(a.await(), b.await())
            }
            _carga.value = CargaUi.Exito(
                "A la vez", perfil, pedidos.size, System.currentTimeMillis() - inicio
            )
        }
    }

    // TODO Paso 7: simularError y manejo de errores
}
