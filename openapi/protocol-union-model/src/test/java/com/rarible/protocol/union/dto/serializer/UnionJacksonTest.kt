package com.rarible.protocol.union.dto.serializer

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.protocol.union.dto.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigInteger

class UnionJacksonTest {

    private val mapper = ObjectMapper()
        .registerModule(UnionPrimitivesJacksonModule)
        .registerModule(UnionModelJacksonModule)
        .registerModule(KotlinModule())


    @Test
    fun `eth address`() {
        val ethAddress = EthAddress("123")

        val serialized = mapper.writeValueAsString(ethAddress)
        assertEquals("\"ETHEREUM:123\"", serialized)

        val deserialized = mapper.readValue(serialized, EthAddress::class.java)
        assertEquals(ethAddress, deserialized)
    }

    @Test
    fun `flow address`() {
        val flowAddress = FlowAddress("123")

        val serialized = mapper.writeValueAsString(flowAddress)
        assertEquals("\"FLOW:123\"", serialized)

        val deserialized = mapper.readValue(serialized, FlowAddress::class.java)
        assertEquals(flowAddress, deserialized)
    }

    @Test
    fun `flow contract`() {
        val flowContract = FlowContract("123:abc")

        val serialized = mapper.writeValueAsString(flowContract)
        assertEquals("\"FLOW:123:abc\"", serialized)

        val deserialized = mapper.readValue(serialized, FlowContract::class.java)
        assertEquals(flowContract, deserialized)
    }

    @Test
    fun `eth itemId`() {
        val itemId = EthItemIdDto(
            value = "abc:123",
            token = EthAddress("abc"),
            tokenId = BigInteger("123")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals("""{"value":"ETHEREUM:abc:123","token":"ETHEREUM:abc","tokenId":123}""", serialized)

        val deserialized = mapper.readValue(serialized, EthItemIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `flow itemId`() {
        val itemId = FlowItemIdDto(
            value = "abc:123",
            token = FlowAddress("abc"),
            tokenId = BigInteger("123")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals("""{"value":"FLOW:abc:123","token":"FLOW:abc","tokenId":123}""", serialized)

        val deserialized = mapper.readValue(serialized, FlowItemIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `eth ownershipId`() {
        val itemId = EthOwnershipIdDto(
            value = "abc:123",
            token = EthAddress("abc"),
            tokenId = BigInteger("123"),
            owner = EthAddress("xyz")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals(
            """{"value":"ETHEREUM:abc:123","token":"ETHEREUM:abc","tokenId":123,"owner":"ETHEREUM:xyz"}""",
            serialized
        )

        val deserialized = mapper.readValue(serialized, EthOwnershipIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `flow ownershipId`() {
        val itemId = FlowOwnershipIdDto(
            value = "abc:123",
            token = FlowContract("abc"),
            tokenId = BigInteger("123"),
            owner = FlowAddress("xyz")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals("""{"value":"FLOW:abc:123","token":"FLOW:abc","tokenId":123,"owner":"FLOW:xyz"}""", serialized)

        val deserialized = mapper.readValue(serialized, FlowOwnershipIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

}