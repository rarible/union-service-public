package com.rarible.protocol.union.enrichment.configuration

import com.simplehash.v0.nft
import io.confluent.kafka.serializers.AbstractKafkaAvroDeserializer
import org.apache.avro.message.RawMessageDecoder
import org.apache.avro.specific.SpecificData
import org.apache.kafka.common.serialization.Deserializer
import java.nio.ByteBuffer


class SHKafkaAvroDeserializer : AbstractKafkaAvroDeserializer(), Deserializer<nft> {

    override fun deserialize(topic: String, bytes: ByteArray): nft? {
        validateSchema(bytes)
        val decoder = RawMessageDecoder<nft>(SpecificData(), nft.`SCHEMA$`)

        // raw message is located starting from the fifth byte
        val withoutSchema = ByteBuffer.wrap(bytes.copyOfRange(5, bytes.size))
        return decoder.decode(withoutSchema)
    }

    override fun close() { }

    // We must validate version of schema
    // If version of schema was changed we need to generate new models from avro model
    private fun validateSchema(bytes: ByteArray) {

        // 0 byte -- just starter marker
        // 1-4 bytes -- version of schema
        val id = ByteBuffer.wrap(bytes.copyOfRange(1, 5)).getInt()
        if (id != SCHEME_V0) {
            throw IllegalArgumentException("Version $id is not supported, must be $SCHEME_V0")
        }
    }

    companion object {
        const val SCHEME_V0 = 100006
    }
}
