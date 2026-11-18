package com.alburquequearzapalo.centrotareas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alburquequearzapalo.centrotareas.data.TareasRepositoryLocal
import com.alburquequearzapalo.centrotareas.ui.PantallaTareas
import com.alburquequearzapalo.centrotareas.ui.theme.CentroTareasTheme
import com.alburquequearzapalo.centrotareas.viewmodel.TareasViewModel
import com.alburquequearzapalo.centrotareas.viewmodel.TareasViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CentroTareasTheme {
                val vm: TareasViewModel = viewModel(
                    factory = TareasViewModelFactory(TareasRepositoryLocal())
                )
                PantallaTareas(vm)
            }
        }
    }
}
