package com.rarible.protocol.union.core.domain

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.rarible.marketplace.core.model.Blockchain
import java.io.Serializable

@JsonSerialize(using = BlockchainIdSerializer::class)
@JsonDeserialize(using = BlockchainIdDeserializer::class)
class BlockchainId(
    val blockchain: Blockchain,
    val id: String
) : Serializable {

    override fun toString(): String = blockchain.formatId(id)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BlockchainId

        if (blockchain != other.blockchain) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = blockchain.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}

fun String.toBlockchainId(): BlockchainId {
    val parts = split("-")
    if (parts.size == 1) {
        return BlockchainId(Blockchain.ETHEREUM, parts[0])
    }
    return BlockchainId(Blockchain.valueOf(parts[0]), parts[1])
}

class BlockchainIdSerializer : StdSerializer<BlockchainId>(BlockchainId::class.java) {
    override fun serialize(value: BlockchainId?, gen: JsonGenerator?, provider: SerializerProvider?) {
        value?.let {
            gen?.writeString(it.toString())
        }
    }
}

class BlockchainIdDeserializer : StdDeserializer<BlockchainId>(BlockchainId::class.java) {
    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): BlockchainId {
        return p!!.let {
            when (it.currentToken) {
                JsonToken.VALUE_STRING -> it.text.toBlockchainId()
                else -> ctxt?.handleUnexpectedToken(_valueClass, p) as BlockchainId
            }
        }
    }
}
