package com.example.aerolineaidle

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object GameStateSerializer : Serializer<GameStateProto> {
    override val defaultValue: GameStateProto = GameStateProto.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): GameStateProto {
        try {
            return GameStateProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: GameStateProto, output: OutputStream) = t.writeTo(output)
}