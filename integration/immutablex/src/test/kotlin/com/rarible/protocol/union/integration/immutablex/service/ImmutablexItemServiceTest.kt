package com.rarible.protocol.union.integration.immutablex.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexItemConverter
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMintsPage
import io.mockk.coEvery
import io.mockk.mockk
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ImmutablexItemServiceTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val itemId = "0xacb3c6a43d15b907e8433077b6d38ae40936fe2c:3832899966304989233"

    private val fullItemId = "${BlockchainDto.IMMUTABLEX}:$itemId"

    private val expectedAsset by lazy {
        mapper.readValue(
            ImmutablexItemServiceTest::class.java.getResourceAsStream("asset.json"),
            ImmutablexAsset::class.java
        )
    }

    private val expectedMint by lazy {
        mapper.readValue(
            ImmutablexItemServiceTest::class.java.getResourceAsStream("mint.json"),
            ImmutablexMint::class.java
        )

    }

    val client = mockk<ImmutablexApiClient> {
        coEvery { getAsset(any()) } returns expectedAsset
        coEvery { getMints(any(), any(), any(), any(), any(), any(), any()) } returns ImmutablexMintsPage(
            "",
            false,
            listOf(expectedMint)
        )
    }

    private val service = ImmutablexItemService(client, ImmutablexItemConverter(client))


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
            Assertions.assertFalse(item.creators.isEmpty())
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
