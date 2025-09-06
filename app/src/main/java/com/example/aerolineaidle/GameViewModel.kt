package com.example.aerolineaidle

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.pow

// ======= MODELO DE JUEGO =======

data class GameState(
    val money: Double = 0.0,
    val planes: Int = 0,
    val routes: Int = 0,
    val clickPower: Double = 1.0,      // Dinero por click
    val opsMultiplier: Double = 1.0,   // Multiplicador de ingresos pasivos
    // lastSavedAt ya no es necesario aquí si el ViewModel maneja la persistencia.
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

// Helper para SharedPreferences, movido aquí para cohesión
private fun SharedPreferences.Editor.putDouble(key: String, value: Double) =
    putLong(key, java.lang.Double.doubleToRawLongBits(value))

private fun SharedPreferences.getDouble(key: String, def: Double): Double =
    java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(def)))


class GameViewModel(application: Application) : ViewModel() {

    private val appContext: Context = application.applicationContext

    // ======= STORAGE SENCILLO (SharedPreferences) =======
    private object GameStorage { // Anidado o como dependencia
        private const val PREFS = "airline_idle_prefs"
        private const val K_MONEY = "money"
        private const val K_PLANES = "planes"
        private const val K_ROUTES = "routes"
        private const val K_CLICK = "click"
        private const val K_OPS = "ops"

        fun load(ctx: Context): GameState {
            val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return GameState(
                money = sp.getDouble(K_MONEY, 0.0),
                planes = sp.getInt(K_PLANES, 0),
                routes = sp.getInt(K_ROUTES, 0),
                clickPower = sp.getDouble(K_CLICK, 1.0),
                opsMultiplier = sp.getDouble(K_OPS, 1.0)
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

    private val _gameState = MutableStateFlow(GameStorage.load(appContext))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var revenueTickerJob: Job? = null
    private var autoSaveJob: Job? = null

    init {
        startRevenueTicker()
        startAutoSave()
    }

    // ======= LÓGICA DE CÁLCULO =======
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

    // ======= ACCIONES DEL JUEGO =======
    fun onManualClick() {
        val currentMoney = _gameState.value.money
        val clickPower = _gameState.value.clickPower
        _gameState.update { it.copy(money = currentMoney + clickPower) }
    }

    fun buyPlane() {
        val cost = planeCost()
        if (_gameState.value.money >= cost) {
            _gameState.update {
                it.copy(
                    money = it.money - cost,
                    planes = it.planes + 1
                )
            }
        }
    }

    fun buyRoute() {
        val cost = routeCost()
        if (_gameState.value.money >= cost) {
            _gameState.update {
                it.copy(
                    money = it.money - cost,
                    routes = it.routes + 1
                )
            }
        }
    }

    fun upgradeClickPower() {
        val cost = clickUpgradeCost()
        if (_gameState.value.money >= cost) {
            _gameState.update {
                it.copy(
                    money = it.money - cost,
                    clickPower = (it.clickPower + 1.0).coerceAtMost(50.0)
                )
            }
        }
    }

    fun upgradeOpsMultiplier() {
        val cost = opsUpgradeCost()
        if (_gameState.value.money >= cost) {
            _gameState.update {
                it.copy(
                    money = it.money - cost,
                    opsMultiplier = (it.opsMultiplier + 0.1).coerceAtMost(10.0) // Cuidado con la precisión de Double
                )
            }
        }
    }
    
    fun resetGame() {
        _gameState.value = GameState() // Restablece al estado inicial
        // El auto-guardado se encargará de persistir esto.
    }

    // ======= TICKERS INTERNOS =======
    private fun startRevenueTicker() {
        revenueTickerJob?.cancel() // Cancela el job anterior si existe
        revenueTickerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val rps = revenuePerSecond()
                if (rps > 0) { // Solo actualiza si hay ingresos para evitar escrituras innecesarias
                    _gameState.update { it.copy(money = it.money + rps) }
                }
            }
        }
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            // Recolecta cambios en gameState, espera 5s de inactividad, luego guarda.
            // O una versión más simple que guarda cada 5s independientemente.
            // Para simplicidad, y replicar el comportamiento original:
            while(true) {
                delay(5000) // Espera 5 segundos
                GameStorage.save(appContext, _gameState.value)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        revenueTickerJob?.cancel()
        autoSaveJob?.cancel()
        // Opcionalmente, podrías querer hacer un último guardado aquí
        // GameStorage.save(appContext, _gameState.value)
        // pero el auto-guardado periódico debería ser suficiente.
    }
}