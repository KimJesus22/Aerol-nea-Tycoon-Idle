package com.example.aerolineaidle

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.delay
import kotlin.math.pow

// ======= MODELO DE JUEGO =======

data class GameState(
    val money: Double = 0.0,
    val planes: Int = 0,
    val routes: Int = 0,
    val clickPower: Double = 1.0,      // Dinero por click
    val opsMultiplier: Double = 1.0,   // Multiplicador de ingresos pasivos
    val lastSavedAt: Long = 0L
)

// Costos base y progresión
private const val BASE_PLANE_COST = 100.0
private const val BASE_ROUTE_COST = 50.0
private const val BASE_CLICK_UPGRADE_COST = 200.0
private const val BASE_OPS_UPGRADE_COST = 300.0

private const val GROWTH = 1.15  // 15% más caro cada compra

// Producción
private const val BASE_RPS_PER_ROUTE = 2.0   // ingresos base por ruta por segundo
private const val BASE_RPS_PER_PLANE = 5.0   // ingresos base por avión por segundo

fun planeCost(nOwned: Int): Double = BASE_PLANE_COST * GROWTH.pow(nOwned)
fun routeCost(nOwned: Int): Double = BASE_ROUTE_COST * GROWTH.pow(nOwned)
fun clickUpgradeCost(power: Double): Double {
    val level = (power - 1.0).coerceAtLeast(0.0)
    return BASE_CLICK_UPGRADE_COST * 1.25.pow(level)
}
fun opsUpgradeCost(mult: Double): Double {
    val level = (mult - 1.0).coerceAtLeast(0.0)
    return BASE_OPS_UPGRADE_COST * 1.35.pow(level)
}

fun revenuePerSecond(state: GameState): Double {
    val fromRoutes = state.routes * BASE_RPS_PER_ROUTE
    val fromPlanes = state.planes * BASE_RPS_PER_PLANE
    return (fromRoutes + fromPlanes) * state.opsMultiplier
}

// ======= STORAGE SENCILLO (SharedPreferences) =======

private const val PREFS = "airline_idle_prefs"
private const val K_MONEY = "money"
private const val K_PLANES = "planes"
private const val K_ROUTES = "routes"
private const val K_CLICK = "click"
private const val K_OPS = "ops"

object GameStorage {
    fun load(ctx: Context): GameState {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return GameState(
            money = sp.getDouble(K_MONEY, 0.0),
            planes = sp.getInt(K_PLANES, 0),
            routes = sp.getInt(K_ROUTES, 0),
            clickPower = sp.getDouble(K_CLICK, 1.0),
            opsMultiplier = sp.getDouble(K_OPS, 1.0),
            lastSavedAt = System.currentTimeMillis()
        )
    }

    fun save(ctx: Context, state: GameState) {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        sp.edit()
            .putDouble(K_MONEY, state.money)
            .putInt(K_PLANES, state.planes)
            .putInt(K_ROUTES, state.routes)
            .putDouble(K_CLICK, state.clickPower)
            .putDouble(K_OPS, state.opsMultiplier)
            .apply()
    }
}

private fun SharedPreferences.Editor.putDouble(key: String, value: Double) =
    putLong(key, java.lang.Double.doubleToRawLongBits(value))

private fun SharedPreferences.getDouble(key: String, def: Double): Double =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(def)))

// ======= ACTIVIDAD PRINCIPAL + UI =======

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = GameStorage.load(this)
        setContent {
            MaterialTheme {
                AirlineIdleApp(initial, appContext = this)
            }
        }
    }
}

@Composable
fun AirlineIdleApp(initial: GameState, appContext: Context) {
    var state by remember { mutableStateOf(initial) }

    // Ticker: suma ingresos cada segundo
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            val rps = revenuePerSecond(state)
            state = state.copy(money = state.money + rps)
        }
    }

    // Auto-guardado cada 5 s (debounce)
    LaunchedEffect(state) {
        delay(5000)
        GameStorage.save(appContext, state.copy(lastSavedAt = System.currentTimeMillis()))
    }

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

            val rps = revenuePerSecond(state)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Dinero: ${formatMoney(state.money)}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(text = "Ingresos/seg: ${formatMoney(rps)}")
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { state = state.copy(money = state.money + state.clickPower) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Despegar (+${formatMoney(state.clickPower)})") }

            Spacer(Modifier.height(16.dp))

            ShopSection(state = state) { newState -> state = newState }

            Spacer(Modifier.height(24.dp))

            TextButton(onClick = {
                state = GameState()
                GameStorage.save(appContext, state)
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
fun ShopSection(state: GameState, onBuy: (GameState) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("Tienda", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        // Comprar Ruta
        val costRoute = routeCost(state.routes)
        ShopItem(
            title = "Nueva Ruta",
            desc = "+${formatMoney(BASE_RPS_PER_ROUTE)} RPS",
            cost = costRoute,
            owned = state.routes,
            canBuy = state.money >= costRoute
        ) {
            if (state.money >= costRoute) {
                onBuy(state.copy(
                    money = state.money - costRoute,
                    routes = state.routes + 1
                ))
            }
        }

        // Comprar Avión
        val costPlane = planeCost(state.planes)
        ShopItem(
            title = "Nuevo Avión",
            desc = "+${formatMoney(BASE_RPS_PER_PLANE)} RPS",
            cost = costPlane,
            owned = state.planes,
            canBuy = state.money >= costPlane
        ) {
            if (state.money >= costPlane) {
                onBuy(state.copy(
                    money = state.money - costPlane,
                    planes = state.planes + 1
                ))
            }
        }

        // Mejora: Click Power
        val costClick = clickUpgradeCost(state.clickPower)
        ShopItem(
            title = "Mejora de Despegue",
            desc = "Aumenta dinero por click",
            cost = costClick,
            owned = state.clickPower.toInt(),
            canBuy = state.money >= costClick
        ) {
            if (state.money >= costClick) {
                onBuy(state.copy(
                    money = state.money - costClick,
                    clickPower = (state.clickPower + 1.0).coerceAtMost(50.0)
                ))
            }
        }

        // Mejora: Operaciones (multiplicador)
        val costOps = opsUpgradeCost(state.opsMultiplier)
        ShopItem(
            title = "Mejora de Operaciones",
            desc = "Multiplica ingresos pasivos",
            cost = costOps,
            owned = (state.opsMultiplier * 10).toInt(),
            canBuy = state.money >= costOps
        ) {
            if (state.money >= costOps) {
                onBuy(state.copy(
                    money = state.money - costOps,
                    opsMultiplier = (state.opsMultiplier + 0.1).coerceAtMost(10.0)
                ))
            }
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
