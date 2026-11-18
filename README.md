# CentroTareas

**Laboratorio 14 · Hilos en Android** — Programación en Móviles (Tecsup) · Docente: Juan León S.
**Autora:** Kiara Alburqueque ([@kiaraalburqueque-stack](https://github.com/kiaraalburqueque-stack))

## Descripción

CentroTareas es una app Android hecha con **Kotlin + Jetpack Compose + Material 3** (sin XML) que ejecuta tres trabajos que tardan, **sin congelar nunca la pantalla**:

1. **Tarea con progreso**: barra de 0 a 100 % en unos 4 s, que se puede cancelar.
2. **Contar números primos**: cálculo pesado en `Dispatchers.Default`, que también se puede cancelar.
3. **Dos cargas simuladas** (perfil y pedidos): una por una (≈ 5 s) contra a la vez con `async` (≈ 3 s), con manejo de errores y botón Reintentar.

Usa corrutinas (`viewModelScope`, `launch`, `withContext`, `async`, `Job`) y la arquitectura **MVVM + Repository**. Todos los datos son ficticios.

## Árbol de paquetes

```
com.alburquequearzapalo.centrotareas/
├── MainActivity.kt
├── model/
│   └── Estados.kt                  PrimosUi, CargaUi
├── data/
│   ├── TareasRepository.kt         interface
│   └── TareasRepositoryLocal.kt    cargas simuladas y contarPrimos
├── viewmodel/
│   ├── TareasViewModel.kt          lógica en segundo plano (pasos 1 a 7)
│   └── TareasViewModelFactory.kt
└── ui/
    ├── PantallaTareas.kt           las tres tarjetas
    └── theme/
```

## Arquitectura MVVM

```
 View (PantallaTareas)
    | eventos: botones              ^ collectAsState()
    v                               |
 ViewModel (TareasViewModel) --- StateFlow --->
    | viewModelScope.launch { ... }
    v
 TareasRepository (interface) ---> TareasRepositoryLocal
                                    (cargas simuladas y contar primos)
```

| Capa | Conoce a | No hace |
|---|---|---|
| View (`ui/`) | Solo a su ViewModel | No lanza corrutinas ni usa `Thread`; solo muestra estado y avisa eventos |
| ViewModel | Solo a la interface del Repository | No conoce Composables ni `Context`, no bloquea el hilo principal |
| Repository | Nada de la UI | No expone estados de pantalla; sus `suspend` no bloquean el hilo que las llama |

## Observación del Paso 1

Al pulsar **Tarea larga (MAL)** y enseguida **Tócame**, la pantalla deja de responder durante 4 segundos: el contador no suma, los botones no reaccionan y, al terminar, se aplican de golpe los toques pendientes.

**¿Por qué no se ve "Trabajando..."?** Porque `tareaLargaMala()` se ejecuta en el **hilo principal** (UI thread). El texto cambia a "Trabajando...", pero el hilo principal queda dormido con `Thread.sleep(4000)` y no puede volver a dibujar la pantalla. Cuando despierta, el mensaje ya cambió a "Terminó", así que solo se dibuja el estado final.

**¿Y si la espera fuera de 10 segundos?** Android detectaría que la app no responde a los eventos de entrada durante más de 5 segundos y mostraría el diálogo **ANR** (*Application Not Responding*: "CentroTareas no responde", con las opciones *Esperar* o *Cerrar app*).

## Capturas

| Tarea con progreso | Tarea cancelada | Primos |
|---|---|---|
| ![Progreso](capturas/01-progreso.png) | ![Cancelada](capturas/02-cancelada.png) | ![Primos](capturas/03-primos.png) |

| Una por una contra A la vez | Error y Reintentar |
|---|---|
| ![Cargas](capturas/04-cargas.png) | ![Error](capturas/05-error.png) |

## Pruebas de aceptación

| Prueba | Resultado esperado | OK |
|---|---|---|
| Tarea larga: tocar Tócame mientras trabaja | El contador sigue sumando | ☐ |
| Logcat con filtro `HILO` | `main` antes y después; `DefaultDispatcher-worker` dentro | ☐ |
| Iniciar la tarea con progreso | La barra avanza de 0 a 100 % en ≈ 4 s | ☐ |
| Cancelar a mitad | La barra se detiene, dice Cancelado y se puede iniciar otra vez | ☐ |
| Pulsar Iniciar dos veces seguidas | No se lanzan dos tareas (el botón está deshabilitado) | ☐ |
| Primos hasta 1000 | Resultado 168 | ☐ |
| Primos con N grande | La pantalla no se congela y Cancelar funciona | ☐ |
| Una por una | ≈ 5 s y resultado correcto | ☐ |
| A la vez | ≈ 3 s y resultado correcto | ☐ |
| Error simulado + Reintentar | Recuadro rojo; al desactivar el interruptor y reintentar, éxito | ☐ |
| Rotar la pantalla durante la tarea | La tarea continúa y el progreso no se reinicia | ☐ |
| Revisar el código | `git grep` no encuentra `GlobalScope`, `runBlocking` ni `Thread.sleep` | ☐ |

## Para pensar

**1. ¿Por qué no se veía "Trabajando..." en el Paso 1?**
Porque el hilo principal es el único que dibuja la pantalla. `Thread.sleep(4000)` lo dejó bloqueado justo después de cambiar el texto, así que no hubo ningún cuadro (frame) en el que se pudiera dibujar "Trabajando...". Cuando el hilo se liberó, el texto ya era "Terminó".

**2. ¿Qué diferencia hay entre `Thread.sleep` y `delay`?**
`Thread.sleep` **bloquea el hilo**: nadie más puede usarlo mientras duerme. `delay` es una función `suspend` que **suspende la corrutina sin bloquear el hilo**: el hilo queda libre para otras tareas (por ejemplo, dibujar) y la corrutina se reanuda cuando pasa el tiempo. Además, `delay` revisa la cancelación; `Thread.sleep` no.

**3. ¿Por qué `Dispatchers.Default` para los primos y `Dispatchers.IO` para la red?**
Contar primos es trabajo de **CPU**: `Default` tiene tantos hilos como núcleos, ideal para cálculos. La red o el disco son trabajo de **espera (E/S)**: el hilo pasa la mayor parte del tiempo esperando la respuesta, por eso `IO` tiene un grupo de hilos más grande, pensado para muchas esperas a la vez sin acaparar los núcleos.

**4. ¿Qué pasaría con la corrutina si el usuario cierra la pantalla? ¿Qué papel cumple `viewModelScope`?**
`viewModelScope` está atado al ciclo de vida del ViewModel. Si el usuario **rota** la pantalla, el ViewModel sobrevive y la corrutina sigue (por eso el progreso no se reinicia). Si el usuario **cierra** la pantalla definitivamente, se llama a `onCleared()` y `viewModelScope` **cancela automáticamente** todas sus corrutinas, así no quedan trabajos huérfanos gastando batería ni fugas de memoria. Por eso está prohibido `GlobalScope`: sus corrutinas no se cancelan nunca.

**5. ¿Por qué `async` tarda 3 s y no 5 s? ¿Qué pasa si una de las dos cargas falla?**
Con `async` las dos cargas empiezan **al mismo tiempo**, y el total es el de la más lenta: máximo(2 s, 3 s) ≈ 3 s. Una por una se suman: 2 s + 3 s ≈ 5 s. Si una falla, como están dentro de `coroutineScope`, la excepción **cancela a la otra** (concurrencia estructurada) y se propaga al `try/catch` del ViewModel, que muestra el estado **Error** con el botón **Reintentar**. La app no se cierra.

## Commits de la Fase 1 (rama `main`)

| N | Mensaje |
|---|---|
| 1 | `chore: importa proyecto base CentroTareas` |
| 2 | `feat(hilos): provoca el congelamiento con Thread.sleep en el hilo principal` |
| 3 | `fix(hilos): mueve la tarea larga a una corrutina con withContext` |
| 4 | `feat(progreso): agrega barra de progreso con StateFlow` |
| 5 | `feat(cancelar): permite cancelar la tarea con Job` |
| 6 | `feat(primos): cuenta primos en Dispatchers.Default con cancelacion` |
| 7 | `feat(carga): compara carga secuencial y en paralelo con async` |
| 8 | `feat(errores): maneja errores de carga y agrega reintento` |
| 9 | `docs: agrega README con observaciones y capturas` |
