package com.example.aerolineaidle

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

// Serializer para GameStateProto
object GameStateSerializer : Serializer<GameStateProto> {
    // Valor por defecto cuando aún no existe un archivo .pb
    override val defaultValue: GameStateProto = GameStateProto.getDefaultInstance()

    // Leer los bytes y convertirlos en una instancia de GameStateProto
    override suspend fun readFrom(input: InputStream): GameStateProto {
        try {
            return GameStateProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            // Convierte cualquier error de parseo en una excepción de corrupción
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    // Serializa GameStateProto y lo escribe en el archivo
    override suspend fun writeTo(t: GameStateProto, output: OutputStream) =
        t.writeTo(output)
}