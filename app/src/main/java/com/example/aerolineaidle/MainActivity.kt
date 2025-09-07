package com.example.aerolineaidle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// La clase GameStateModel, constantes, funciones de cálculo y GameStorage
// han sido movidos a GameViewModel.kt

// ======= ACTIVIDAD PRINCIPAL + UI =======

class MainActivity : ComponentActivity() {
    // Obtener instancia del ViewModel
    private val gameViewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // Observar el gameState del ViewModel
                val gameState by gameViewModel.gameState.collectAsStateWithLifecycle()
                AirlineIdleApp(
                    gameState = gameState,
                    viewModel = gameViewModel
                )
            }
        }
    }
}

@Composable
fun AirlineIdleApp(gameState: GameStateModel, viewModel: GameViewModel) {
    // Los LaunchedEffect para el ticker de ingresos y el auto-guardado
    // ya no son necesarios aquí, el ViewModel los maneja.

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Aerolínea Idle",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))

            val rps = viewModel.revenuePerSecond() // Usar método del ViewModel
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Dinero: ${formatMoney(gameState.money)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(text = "Ingresos/seg: ${formatMoney(rps)}")
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { viewModel.onManualClick() }, // Usar método del ViewModel
                modifier = Modifier.fillMaxWidth()
            ) { Text("Despegar (+${formatMoney(gameState.clickPower)})") }

            Spacer(Modifier.height(16.dp))

            ShopSection(gameState = gameState, viewModel = viewModel)

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = {
                viewModel.resetGame() // Usar método del ViewModel
            }) { Text("Reiniciar progreso") }

            Spacer(Modifier.height(12.dp))
            Text(
                "Consejo: el costo sube con cada compra. Invierte en rutas para RPS temprano; luego aviones y mejoras.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ShopSection(gameState: GameStateModel, viewModel: GameViewModel) {
    Column(Modifier.fillMaxWidth()) {
        Text("Tienda", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // Comprar Ruta
        val costRoute = viewModel.routeCost() // Usar método del ViewModel
        ShopItem(
            title = "Nueva Ruta",
            // Las constantes como BASE_RPS_PER_ROUTE deben estar en ViewModel si se necesitan dinámicamente,
            // o ser valores fijos si la descripción no cambia.
            // Para este ejemplo, asumimos que el ViewModel podría exponer estas constantes si fuera necesario,
            // o la UI usa valores estáticos que conoce.
            // Por simplicidad, si BASE_RPS_PER_ROUTE (2.0) es fijo para la UI:
            desc = "+${formatMoney(2.0)} RPS", // Ajustar si es necesario
            cost = costRoute,
            owned = gameState.routes,
            canBuy = gameState.money >= costRoute
        ) {
            viewModel.buyRoute() // Usar método del ViewModel
        }

        // Comprar Avión
        val costPlane = viewModel.planeCost() // Usar método del ViewModel
        ShopItem(
            title = "Nuevo Avión",
            desc = "+${formatMoney(5.0)} RPS", // Ajustar si es necesario (BASE_RPS_PER_PLANE)
            cost = costPlane,
            owned = gameState.planes,
            canBuy = gameState.money >= costPlane
        ) {
            viewModel.buyPlane() // Usar método del ViewModel
        }

        // Mejora: Click Power
        val costClick = viewModel.clickUpgradeCost() // Usar método del ViewModel
        ShopItem(
            title = "Mejora de Despegue",
            desc = "Aumenta dinero por click",
            cost = costClick,
            owned = gameState.clickPower.toInt(), // El nivel se podría calcular en ViewModel también
            canBuy = gameState.money >= costClick
        ) {
            viewModel.upgradeClickPower() // Usar método del ViewModel
        }

        // Mejora: Operaciones (multiplicador)
        val costOps = viewModel.opsUpgradeCost() // Usar método del ViewModel
        ShopItem(
            title = "Mejora de Operaciones",
            desc = "Multiplica ingresos pasivos",
            cost = costOps,
            owned = (gameState.opsMultiplier * 10).toInt(), // El nivel se podría calcular en ViewModel
            canBuy = gameState.money >= costOps
        ) {
            viewModel.upgradeOpsMultiplier() // Usar método del ViewModel
        }
    }
}

@Composable
fun ShopItem(
    title: String,
    desc: String,
    cost: Double,
    owned: Int,
    canBuy: Boolean,
    onBuy: () -> Unit
) {
    Card(Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(desc, style = MaterialTheme.typography.bodySmall)
                }
                Text("Coste: ${formatMoney(cost)}", modifier = Modifier.padding(end = 12.dp))
                Button(onClick = onBuy, enabled = canBuy) { Text("Comprar") }
            }
            Text("Poseídos: $owned", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ======= UTIL =======

fun formatMoney(x: Double): String =
    when {
        x >= 1_000_000_000 -> String.format("%.2fB", x / 1_000_000_000)
        x >= 1_000_000     -> String.format("%.2fM", x / 1_000_000)
        x >= 1_000         -> String.format("%.1fk", x / 1_000)
        else               -> String.format("%.0f", x)
    }
