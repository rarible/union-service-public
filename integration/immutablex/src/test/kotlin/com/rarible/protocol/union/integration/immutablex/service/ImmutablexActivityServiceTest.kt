package com.rarible.protocol.union.integration.immutablex.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexActivityConverter
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfer
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class ImmutablexActivityServiceTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val expectedMintActivity by lazy {
        ImmutablexPage<ImmutablexMint>("", false,
            listOf(
                mapper.readValue(
                    ImmutablexActivityServiceTest::class.java.getResourceAsStream("mint.json"),
                    ImmutablexMint::class.java
                )
            )
        )
    }

    private val expectedTransfersActivity by lazy {
        ImmutablexPage<ImmutablexTransfer>("", false,
            listOf(
                mapper.readValue(
                    ImmutablexActivityServiceTest::class.java.getResourceAsStream("transfer.json"),
                    ImmutablexTransfer::class.java
                )
            )
        )
    }

    private val expectedTradesActivity by lazy {
        ImmutablexPage<ImmutablexTrade>("", false,
            listOf(
                mapper.readValue(
                    ImmutablexActivityServiceTest::class.java.getResourceAsStream("trade.json"),
                    ImmutablexTrade::class.java
                )
            )
        )
    }

    private val service = ImmutablexActivityService(
        mockk {
            coEvery { getMints(any(), any(), any(), any(), any(), any()) } returns expectedMintActivity
            coEvery { getTransfers(any(), any(), any(), any(), any(), any()) } returns expectedTransfersActivity
            coEvery { getTrades(any(), any(), any(), any(), any(), any()) } returns expectedTradesActivity
        },
        ImmutablexActivityConverter(BlockchainDto.IMMUTABLEX),
    )

    @Test
    fun getAllActivities() = runBlocking {
        service.getAllActivities(
            listOf(ActivityTypeDto.MINT, ActivityTypeDto.TRANSFER, ActivityTypeDto.SELL),
            null,
            50,
            null
        ).let { page ->
            Assertions.assertEquals(page.entities.size, 3)
            assert(page.entities[0] is MintActivityDto)
            assert(page.entities[1] is OrderMatchSellDto)
            assert(page.entities[2] is TransferActivityDto)
        }

        service.getAllActivities(listOf(ActivityTypeDto.MINT), null, 50, null)
            .let { page ->
                Assertions.assertEquals(page.entities.size, 1)
                val activity = page.entities.single() as MintActivityDto
                val expected = expectedMintActivity.result.single()
                Assertions.assertEquals(activity.tokenId, expected.token.data.tokenId!!.toBigInteger())
                Assertions.assertEquals(activity.owner.value, expected.user)
            }

        service.getAllActivities(listOf(ActivityTypeDto.SELL), null, 50, null)
            .let { page ->
                Assertions.assertEquals(page.entities.size, 1)
                val activity = page.entities.single() as OrderMatchSellDto
                val expected = expectedTradesActivity.result.single()
                Assertions.assertEquals(activity.sellerOrderHash, expected.make.orderId.toString())
                Assertions.assertEquals(activity.buyerOrderHash, expected.take.orderId.toString())
            }

        service.getAllActivities(listOf(ActivityTypeDto.TRANSFER), null, 50, null)
            .let { page ->
                Assertions.assertEquals(page.entities.size, 1)
                val activity = page.entities.single() as TransferActivityDto
                val expected = expectedTransfersActivity.result.single()
                Assertions.assertEquals(activity.tokenId, expected.token.data.tokenId!!.toBigInteger())
                Assertions.assertEquals(activity.value, expected.token.data.quantity?.toBigInteger())
                Assertions.assertEquals(activity.from.value, expected.user)
                Assertions.assertEquals(activity.owner.value, expected.receiver)
            }
    }

    @Test
    fun getActivitiesByItem() = runBlocking {
        val itemId = expectedMintActivity.result.single().token.data.tokenId!!

        service.getActivitiesByItem(
            listOf(ActivityTypeDto.MINT),
            itemId,
            null,
            50,
            null
        ).let { page ->
            println(page.entities)
            Assertions.assertEquals(page.entities.size, 1)
            assert(page.entities[0] is MintActivityDto)
        }
    }

    @Test
    fun getActivitiesByUser() = runBlocking {
        val userId = expectedMintActivity.result.single().user
        service.getActivitiesByUser(
            types = listOf(UserActivityTypeDto.MINT),
            users = listOf(userId),
            from = null,
            to = null,
            continuation = null,
            size = 50,
            sort = null
        ).let { page ->
            println(page.entities)
            Assertions.assertEquals(page.entities.size, 1)
            assert(page.entities[0] is MintActivityDto)
        }
    }

}
