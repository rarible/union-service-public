package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
        val ethAddress = UnionAddress(BlockchainDto.ETHEREUM, "123")

        val serialized = mapper.writeValueAsString(ethAddress)
        assertEquals("\"ETHEREUM:123\"", serialized)

        val deserialized = mapper.readValue(serialized, UnionAddress::class.java)
        assertEquals(ethAddress, deserialized)
    }

    @Test
    fun `flow address`() {
        val flowAddress = UnionAddress(BlockchainDto.FLOW, "123")

        val serialized = mapper.writeValueAsString(flowAddress)
        assertEquals("\"FLOW:123\"", serialized)

        val deserialized = mapper.readValue(serialized, UnionAddress::class.java)
        assertEquals(flowAddress, deserialized)
    }

    @Test
    fun `flow contract`() {
        val flowContract = UnionAddress(BlockchainDto.FLOW, "123:abc")

        val serialized = mapper.writeValueAsString(flowContract)
        assertEquals("\"FLOW:123:abc\"", serialized)

        val deserialized = mapper.readValue(serialized, UnionAddress::class.java)
        assertEquals(flowContract, deserialized)
    }

    @Test
    fun `eth itemId`() {
        val itemId = ItemIdDto(
            blockchain = BlockchainDto.POLYGON,
            token = UnionAddress(BlockchainDto.POLYGON, "abc"),
            tokenId = BigInteger("123")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals(
            """{"value":"POLYGON:abc:123","blockchain":"POLYGON","token":"POLYGON:abc","tokenId":123}""",
            serialized
        )

        val deserialized = mapper.readValue(serialized, ItemIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `flow itemId`() {
        val itemId = ItemIdDto(
            blockchain = BlockchainDto.FLOW,
            token = UnionAddress(BlockchainDto.FLOW, "abc"),
            tokenId = BigInteger("123")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals("""{"value":"FLOW:abc:123","blockchain":"FLOW","token":"FLOW:abc","tokenId":123}""", serialized)

        val deserialized = mapper.readValue(serialized, ItemIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `eth ownershipId`() {
        val itemId = OwnershipIdDto(
            blockchain = BlockchainDto.ETHEREUM,
            token = UnionAddress(BlockchainDto.ETHEREUM, "abc"),
            tokenId = BigInteger("123"),
            owner = UnionAddress(BlockchainDto.ETHEREUM, "xyz")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals(
            """{"value":"ETHEREUM:abc:123:xyz","blockchain":"ETHEREUM","token":"ETHEREUM:abc","tokenId":123,"owner":"ETHEREUM:xyz"}""",
            serialized
        )

        val deserialized = mapper.readValue(serialized, OwnershipIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `flow ownershipId`() {
        val ownershipId = OwnershipIdDto(
            blockchain = BlockchainDto.FLOW,
            token = UnionAddress(BlockchainDto.FLOW, "abc"),
            tokenId = BigInteger("123"),
            owner = UnionAddress(BlockchainDto.FLOW, "xyz")
        )

        val serialized = mapper.writeValueAsString(ownershipId)
        assertEquals(
            """{"value":"FLOW:abc:123:xyz","blockchain":"FLOW","token":"FLOW:abc","tokenId":123,"owner":"FLOW:xyz"}""",
            serialized
        )

        val deserialized = mapper.readValue(serialized, OwnershipIdDto::class.java)
        assertEquals(ownershipId, deserialized)
    }

    @Test
    fun `eth order id`() {
        val ethOrderId = OrderIdDto(BlockchainDto.ETHEREUM, "754")

        val serialized = mapper.writeValueAsString(ethOrderId)
        assertEquals("\"ETHEREUM:754\"", serialized)

        val deserialized = mapper.readValue(serialized, OrderIdDto::class.java)
        assertEquals(ethOrderId, deserialized)
    }

    @Test
    fun `flow order id`() {
        val flowOrderId = OrderIdDto(BlockchainDto.FLOW, "754")

        val serialized = mapper.writeValueAsString(flowOrderId)
        assertEquals("\"FLOW:754\"", serialized)

        val deserialized = mapper.readValue(serialized, OrderIdDto::class.java)
        assertEquals(flowOrderId, deserialized)
    }

    @Test
    fun `eth activity id`() {
        val ethActivityId = ActivityIdDto(BlockchainDto.ETHEREUM, "754")

        val serialized = mapper.writeValueAsString(ethActivityId)
        assertEquals("\"ETHEREUM:754\"", serialized)

        val deserialized = mapper.readValue(serialized, ActivityIdDto::class.java)
        assertEquals(ethActivityId, deserialized)
    }

    @Test
    fun `flow activity id`() {
        val flowActivityId = ActivityIdDto(BlockchainDto.FLOW, "754")

        val serialized = mapper.writeValueAsString(flowActivityId)
        assertEquals("\"FLOW:754\"", serialized)

        val deserialized = mapper.readValue(serialized, ActivityIdDto::class.java)
        assertEquals(flowActivityId, deserialized)
    }

}