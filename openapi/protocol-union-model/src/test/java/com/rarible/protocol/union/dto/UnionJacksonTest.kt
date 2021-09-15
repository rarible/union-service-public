package com.rarible.protocol.union.dto

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.protocol.union.dto.ethereum.*
import com.rarible.protocol.union.dto.flow.*
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
        val ethAddress = EthAddress(EthBlockchainDto.ETHEREUM, "123")

        val serialized = mapper.writeValueAsString(ethAddress)
        assertEquals("\"ETHEREUM:123\"", serialized)

        val deserialized = mapper.readValue(serialized, EthAddress::class.java)
        assertEquals(ethAddress, deserialized)
    }

    @Test
    fun `flow address`() {
        val flowAddress = FlowAddress(FlowBlockchainDto.FLOW, "123")

        val serialized = mapper.writeValueAsString(flowAddress)
        assertEquals("\"FLOW:123\"", serialized)

        val deserialized = mapper.readValue(serialized, FlowAddress::class.java)
        assertEquals(flowAddress, deserialized)
    }

    @Test
    fun `flow contract`() {
        val flowContract = FlowContract(FlowBlockchainDto.FLOW, "123:abc")

        val serialized = mapper.writeValueAsString(flowContract)
        assertEquals("\"FLOW:123:abc\"", serialized)

        val deserialized = mapper.readValue(serialized, FlowContract::class.java)
        assertEquals(flowContract, deserialized)
    }

    @Test
    fun `eth itemId`() {
        val itemId = EthItemIdDto(
            blockchain = EthBlockchainDto.POLYGON,
            token = EthAddress(EthBlockchainDto.POLYGON, "abc"),
            tokenId = BigInteger("123")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals(
            """{"value":"POLYGON:abc:123","blockchain":"POLYGON","token":"POLYGON:abc","tokenId":123}""",
            serialized
        )

        val deserialized = mapper.readValue(serialized, EthItemIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `flow itemId`() {
        val itemId = FlowItemIdDto(
            blockchain = FlowBlockchainDto.FLOW,
            token = FlowContract(FlowBlockchainDto.FLOW, "abc"),
            tokenId = BigInteger("123")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals("""{"value":"FLOW:abc:123","blockchain":"FLOW","token":"FLOW:abc","tokenId":123}""", serialized)

        val deserialized = mapper.readValue(serialized, FlowItemIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `eth ownershipId`() {
        val itemId = EthOwnershipIdDto(
            blockchain = EthBlockchainDto.ETHEREUM,
            token = EthAddress(EthBlockchainDto.ETHEREUM, "abc"),
            tokenId = BigInteger("123"),
            owner = EthAddress(EthBlockchainDto.ETHEREUM, "xyz")
        )

        val serialized = mapper.writeValueAsString(itemId)
        assertEquals(
            """{"value":"ETHEREUM:abc:123:xyz","blockchain":"ETHEREUM","token":"ETHEREUM:abc","tokenId":123,"owner":"ETHEREUM:xyz"}""",
            serialized
        )

        val deserialized = mapper.readValue(serialized, EthOwnershipIdDto::class.java)
        assertEquals(itemId, deserialized)
    }

    @Test
    fun `flow ownershipId`() {
        val ownershipId = FlowOwnershipIdDto(
            blockchain = FlowBlockchainDto.FLOW,
            token = FlowContract(FlowBlockchainDto.FLOW, "abc"),
            tokenId = BigInteger("123"),
            owner = FlowAddress(FlowBlockchainDto.FLOW, "xyz")
        )

        val serialized = mapper.writeValueAsString(ownershipId)
        assertEquals(
            """{"value":"FLOW:abc:123:xyz","blockchain":"FLOW","token":"FLOW:abc","tokenId":123,"owner":"FLOW:xyz"}""",
            serialized
        )

        val deserialized = mapper.readValue(serialized, FlowOwnershipIdDto::class.java)
        assertEquals(ownershipId, deserialized)
    }

    @Test
    fun `eth order id`() {
        val ethOrderId = EthOrderIdDto(EthBlockchainDto.ETHEREUM, "754")

        val serialized = mapper.writeValueAsString(ethOrderId)
        assertEquals("\"ETHEREUM:754\"", serialized)

        val deserialized = mapper.readValue(serialized, EthOrderIdDto::class.java)
        assertEquals(ethOrderId, deserialized)
    }

    @Test
    fun `flow order id`() {
        val flowOrderId = FlowOrderIdDto(FlowBlockchainDto.FLOW, "754")

        val serialized = mapper.writeValueAsString(flowOrderId)
        assertEquals("\"FLOW:754\"", serialized)

        val deserialized = mapper.readValue(serialized, FlowOrderIdDto::class.java)
        assertEquals(flowOrderId, deserialized)
    }

    @Test
    fun `eth activity id`() {
        val ethActivityId = EthActivityIdDto(EthBlockchainDto.ETHEREUM, "754")

        val serialized = mapper.writeValueAsString(ethActivityId)
        assertEquals("\"ETHEREUM:754\"", serialized)

        val deserialized = mapper.readValue(serialized, EthActivityIdDto::class.java)
        assertEquals(ethActivityId, deserialized)
    }

    @Test
    fun `flow activity id`() {
        val flowActivityId = FlowActivityIdDto(FlowBlockchainDto.FLOW, "754")

        val serialized = mapper.writeValueAsString(flowActivityId)
        assertEquals("\"FLOW:754\"", serialized)

        val deserialized = mapper.readValue(serialized, FlowActivityIdDto::class.java)
        assertEquals(flowActivityId, deserialized)
    }

}