package com.rarible.protocol.union.enrichment.meta.simplehash

import com.simplehash.v0.nft
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.message.RawMessageEncoder
import org.apache.avro.specific.SpecificData


class SHKafkaAvroSerializer : KafkaAvroSerializer() {
    override fun serialize(topic: String?, record: Any?): ByteArray {
        val event = record as nft
        val encoder = RawMessageEncoder<nft>(SpecificData(), nft.`SCHEMA$`)
        val encoded = encoder.encode(event)

        // 0 byte -- just starter marker
        // 1-4 bytes -- version of schema
        return byteArrayOf(0, 0, 1, -122, -90) + encoded.array()
    }
}