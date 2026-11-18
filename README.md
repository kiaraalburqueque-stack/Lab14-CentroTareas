# CentroTareas

App Android (Kotlin + Jetpack Compose + Material 3) del Laboratorio 14 · **Hilos en Android** · Programación en Móviles — Tecsup.

## Observación del Paso 1

Al pulsar **Tarea larga (MAL)** y enseguida **Tócame**, la pantalla deja de responder durante 4 segundos: el contador no suma, los botones no reaccionan y, al terminar, se aplican de golpe los toques pendientes.

**¿Por qué no se ve "Trabajando..."?** Porque `tareaLargaMala()` se ejecuta en el **hilo principal** (UI thread). El texto cambia a "Trabajando...", pero el hilo principal queda dormido con `Thread.sleep(4000)` y no puede volver a dibujar la pantalla. Cuando despierta, el mensaje ya cambió a "Terminó", así que solo se dibuja el estado final.

**¿Y si la espera fuera de 10 segundos?** Android detectaría que la app no responde a los eventos de entrada durante más de 5 segundos y mostraría el diálogo **ANR** (*Application Not Responding*: "CentroTareas no responde", con las opciones *Esperar* o *Cerrar app*).
