package com.rarible.protocol.union.integration.immutablex.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMintsPage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.math.BigInteger

class ImmutablexItemServiceTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val itemId = "0xacb3c6a43d15b907e8433077b6d38ae40936fe2c:3832899966304989233"
    private val creator = "0x6663c6a43d15b0987e8433077b6d38ae40936fe33"

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

    val assetClient = mockk<ImmutablexAssetClient> {
        coEvery { getById(any()) } returns expectedAsset
    }

    val activityClient = mockk<ImmutablexActivityClient> {
        coEvery { getItemCreator(itemId) } returns creator
        coEvery { getMints(any(), any(), any(), any(), any(), any(), any()) } returns ImmutablexMintsPage(
            "",
            false,
            listOf(expectedMint)
        )
    }

    private val service = ImmutablexItemService(assetClient, activityClient)

    @Test
    internal fun `get item by id`() {
        runBlocking {
            val item = service.getItemById(itemId)
            assertNotNull(item)
            assertEquals(fullItemId, item.id.fullId())
            assertFalse(item.deleted)
            assertEquals(
                CollectionIdDto(
                    BlockchainDto.IMMUTABLEX,
                    expectedAsset.tokenAddress
                ), item.collection
            )
            assertEquals(expectedAsset.createdAt, item.mintedAt)
            assertEquals(expectedAsset.updatedAt, item.lastUpdatedAt)
            assertEquals(BigInteger.ZERO, item.lazySupply)
            assertEquals(BigInteger.ONE, item.supply)
            assertFalse(item.creators.isEmpty())
        }
    }

    @Test
    internal fun `get item meta by id`() {
        runBlocking {
            val meta = service.getItemMetaById(itemId)
            assertNotNull(meta)
            assertEquals(expectedAsset.name, meta.name)
            assertEquals(expectedAsset.description, meta.description)
            assertFalse(meta.content.isEmpty())
            assertFalse(meta.attributes.isEmpty())
        }
    }
}
