package com.rarible.protocol.union.integration.immutablex.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

class ImmutablexItemServiceTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val itemId = "0xacb3c6a43d15b907e8433077b6d38ae40936fe2c:51267701"

    private val fullItemId = "${BlockchainDto.IMMUTABLEX}:$itemId"

    private val expectedAsset by lazy {
        mapper.readValue(
            ImmutablexItemServiceTest::class.java.getResourceAsStream("asset.json"),
            ImmutablexAsset::class.java
        )
    }

    private val service = ImmutablexItemService(
        mockk {
            coEvery { getAsset(any()) } returns expectedAsset
        }
    )


    @Test
    internal fun `get item by id`() {
        runBlocking {
            val item = service.getItemById(itemId)
            Assertions.assertNotNull(item)
            Assertions.assertEquals(fullItemId, item.id.fullId())
            Assertions.assertFalse(item.deleted)
            Assertions.assertEquals(CollectionIdDto(
                BlockchainDto.IMMUTABLEX,
                expectedAsset.tokenAddress
            ), item.collection)
            Assertions.assertEquals(expectedAsset.createdAt, item.mintedAt)
            Assertions.assertEquals(expectedAsset.updatedAt, item.lastUpdatedAt)
            Assertions.assertEquals(BigInteger.ZERO, item.lazySupply)
            Assertions.assertEquals(BigInteger.ONE, item.supply)
        }
    }

    @Test
    internal fun `get item meta by id`() {
        runBlocking {
            val meta = service.getItemMetaById(itemId)
            Assertions.assertNotNull(meta)
            Assertions.assertEquals(expectedAsset.name, meta.name)
            Assertions.assertEquals(expectedAsset.description, meta.description)
            Assertions.assertFalse(meta.content.isEmpty())
            Assertions.assertFalse(meta.attributes.isEmpty())
        }
    }
}
