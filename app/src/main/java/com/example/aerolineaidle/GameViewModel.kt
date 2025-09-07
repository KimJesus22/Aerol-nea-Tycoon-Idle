package com.example.aerolineaidle

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.pow
// Importar para el manejo de excepciones en DataStore, si se añade
// import java.io.IOException
// import androidx.datastore.core.CorruptionException
// import android.util.Log

// ======= MODELO DE JUEGO =======

data class GameState(
    val money: Double = 0.0,
    val planes: Int = 0,
    val routes: Int = 0,
    val clickPower: Double = 1.0,      // Dinero por click
    val opsMultiplier: Double = 1.0,   // Multiplicador de ingresos pasivos
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

class GameViewModel(application: Application) : ViewModel() {

    private val appContext: Context = application.applicationContext

    private val _gameState = MutableStateFlow(GameState()) 
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var revenueTickerJob: Job? = null

    init {
        viewModelScope.launch {
            appContext.gameStateDataStore.data
                // Aquí se podría añadir .catch para manejar IOException/CorruptionException
                .collect { proto ->
                    _gameState.value = proto.toGameState()
                }
        }
        startRevenueTicker()
    }

    // ======= LÓGICA DE CÁLCULO (depende de _gameState.value, que es actualizado por DataStore) =======
    fun planeCost(): Double = BASE_PLANE_COST * GROWTH.pow(_gameState.value.planes)
    fun routeCost(): Double = BASE_ROUTE_COST * GROWTH.pow(_gameState.value.routes)
    fun clickUpgradeCost(): Double {
        val level = (_gameState.value.clickPower - 1.0).coerceAtLeast(0.0)
        return BASE_CLICK_UPGRADE_COST * 1.25.pow(level)
    }
    fun opsUpgradeCost(): Double {
        val level = (_gameState.value.opsMultiplier - 1.0).coerceAtLeast(0.0)
        return BASE_OPS_UPGRADE_COST * 1.35.pow(level)
    }

    fun revenuePerSecond(): Double {
        val state = _gameState.value
        val fromRoutes = state.routes * BASE_RPS_PER_ROUTE
        val fromPlanes = state.planes * BASE_RPS_PER_PLANE
        return (fromRoutes + fromPlanes) * state.opsMultiplier
    }

    // ======= ACCIONES DEL JUEGO (actualizan DataStore) =======
    fun onManualClick() {
        viewModelScope.launch {
            appContext.gameStateDataStore.updateData { proto ->
                proto.toBuilder()
                    .setMoney(proto.money + proto.clickPower) // proto.clickPower es el valor actual en DataStore
                    .build()
            }
        }
    }

    fun buyPlane() {
        val cost = planeCost()
        if (_gameState.value.money >= cost) { // Verifica con el estado actual de la UI
            viewModelScope.launch {
                appContext.gameStateDataStore.updateData { proto ->
                    proto.toBuilder()
                        .setMoney(proto.money - cost)
                        .setPlanes(proto.planes + 1)
                        .build()
                }
            }
        }
    }

    fun buyRoute() {
        val cost = routeCost()
        if (_gameState.value.money >= cost) {
            viewModelScope.launch {
                appContext.gameStateDataStore.updateData { proto ->
                    proto.toBuilder()
                        .setMoney(proto.money - cost)
                        .setRoutes(proto.routes + 1)
                        .build()
                }
            }
        }
    }

    fun upgradeClickPower() {
        val cost = clickUpgradeCost()
        if (_gameState.value.money >= cost) {
            viewModelScope.launch {
                appContext.gameStateDataStore.updateData { proto ->
                    proto.toBuilder()
                        .setMoney(proto.money - cost)
                        .setClickPower((proto.clickPower + 1.0).coerceAtMost(50.0))
                        .build()
                }
            }
        }
    }

    fun upgradeOpsMultiplier() {
        val cost = opsUpgradeCost()
        if (_gameState.value.money >= cost) {
            viewModelScope.launch {
                appContext.gameStateDataStore.updateData { proto ->
                    proto.toBuilder()
                        .setMoney(proto.money - cost)
                        .setOpsMultiplier((proto.opsMultiplier + 0.1).coerceAtMost(10.0))
                        .build()
                }
            }
        }
    }
    
    fun resetGame() {
        viewModelScope.launch {
            appContext.gameStateDataStore.updateData {
                GameState().toProto() // Usa GameState() por defecto y lo convierte a Proto
            }
        }
    }

    // ======= TICKERS INTERNOS =======
    private fun startRevenueTicker() {
        revenueTickerJob?.cancel()
        revenueTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val rps = revenuePerSecond() // Calcula RPS basado en el estado actual de la UI
                if (rps > 0) {
                    appContext.gameStateDataStore.updateData { proto ->
                        proto.toBuilder()
                            .setMoney(proto.money + rps)
                            .build()
                    }
                }
            }
        }
    }

    // ======= FUNCIONES DE CONVERSIÓN PROTOBUF =======
    private fun GameStateProto.toGameState() = GameState(
        money = money,
        planes = planes,
        routes = routes,
        clickPower = clickPower,
        opsMultiplier = opsMultiplier
    )

    private fun GameState.toProto(): GameStateProto = GameStateProto.newBuilder()
        .setMoney(money)
        .setPlanes(planes)
        .setRoutes(routes)
        .setClickPower(clickPower)
        .setOpsMultiplier(opsMultiplier)
        .build()

    override fun onCleared() {
        super.onCleared()
        revenueTickerJob?.cancel()
    }
}