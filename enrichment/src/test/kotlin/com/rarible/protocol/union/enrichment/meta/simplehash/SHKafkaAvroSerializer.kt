package com.rarible.protocol.union.enrichment.meta.simplehash

import com.simplehash.v0.nft
import io.confluent.kafka.serializers.AbstractKafkaAvroSerializer
import org.apache.avro.message.RawMessageEncoder
import org.apache.avro.specific.SpecificData
import org.apache.kafka.common.serialization.Serializer


class SHKafkaAvroSerializer : AbstractKafkaAvroSerializer(), Serializer<nft> {

    override fun serialize(p0: String?, event: nft?): ByteArray {
        val encoder = RawMessageEncoder<nft>(SpecificData(), nft.`SCHEMA$`)
        val encoded = encoder.encode(event)

        // 0 byte -- just starter marker
        // 1-4 bytes -- version of schema
        return byteArrayOf(0, 0, 1, -122, -90) + encoded.array()
    }

    override fun close() { }
}