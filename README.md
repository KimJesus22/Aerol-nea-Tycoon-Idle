# Aerolinea IDLE

Aerolinea IDLE es un juego incremental para Android donde gestionas y haces crecer tu propia aerolínea. Comienzas con una modesta cantidad de dinero y, a través de clics y compras estratégicas, aumentas tus ingresos para convertirte en un magnate de la aviación.

## Características Principales

*   **Juego Idle:** Genera ingresos pasivamente a través de tus rutas y aviones.
*   **Gestión de Flota:** Compra aviones para aumentar tus ganancias.
*   **Expansión de Rutas:** Adquiere nuevas rutas para incrementar tus ingresos por segundo.
*   **Mejoras:**
    *   **Poder de Click:** Aumenta el dinero obtenido por cada "despegue" manual.
    *   **Eficiencia Operativa:** Multiplica tus ingresos pasivos globales.
*   **Persistencia de Datos:** El progreso del juego se guarda automáticamente utilizando SharedPreferences.
*   **Interfaz de Usuario Moderna:** Construido con Jetpack Compose y Material 3.

## Estructura del Proyecto

El código fuente principal se encuentra en `app/src/main/java/com/example/aerolineaidle/MainActivity.kt`. Este archivo contiene:

*   **`GameState` (Data Class):** Modelo que representa el estado actual del juego (dinero, aviones, rutas, mejoras).
*   **Cálculos del Juego:** Funciones para determinar los costos de las mejoras (aviones, rutas, poder de click, multiplicador de operaciones) y los ingresos por segundo.
*   **`GameStorage` (Objeto):** Se encarga de guardar y cargar el `GameState` utilizando `SharedPreferences`.
*   **`MainActivity` (Activity):** Punto de entrada de la aplicación. Carga el estado del juego e inicializa la UI.
*   **`AirlineIdleApp` (Composable):** Función principal de la UI, construida con Jetpack Compose. Gestiona el estado de la UI y los bucles de actualización del juego (ingresos pasivos y autoguardado).
*   **`ShopSection` y `ShopItem` (Composables):** Componentes de la UI para la tienda donde el jugador puede comprar mejoras.
*   **`formatMoney` (Función Utilidad):** Formatea los valores numéricos grandes (dinero, costos) en un formato legible (ej. 1.5k, 2.3M).

## Configuración del Proyecto

*   **Lenguaje:** Kotlin
*   **UI:** Jetpack Compose con Material 3
*   **Versión de Java/Kotlin:** 17
*   **Dependencias Clave:**
    *   `androidx.compose.material3:material3`
    *   `androidx.activity:activity-compose`
    *   `com.google.android.material:material` (para temas XML de Material 3)
    *   `org.jetbrains.kotlinx:kotlinx-coroutines-android` (para corrutinas)

## Cómo Compilar y Ejecutar

1.  Clona este repositorio.
2.  Abre el proyecto en Android Studio (versión recomendada: Iguana o posterior).
3.  Asegúrate de tener un emulador de Android configurado o un dispositivo físico conectado.
4.  Sincroniza el proyecto con los archivos Gradle si es necesario.
5.  Ejecuta la configuración de la aplicación `app`.

## Posibles Mejoras Futuras

*   Añadir más tipos de mejoras o elementos desbloqueables.
*   Implementar un sistema de logros.
*   Mejorar los gráficos y animaciones.
*   Guardado en la nube.
*   Tabla de clasificación online.