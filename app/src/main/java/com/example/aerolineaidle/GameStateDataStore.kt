package com.example.aerolineaidle

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore

// Define el DataStore como una extensión de Context
val Context.gameStateDataStore: DataStore<GameStateProto> by dataStore(
    fileName = "game_state.pb",
    serializer = GameStateSerializer
)
