package com.rarible.protocol.union.integration.immutablex.service

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMintsPage
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTradesPage
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfersPage
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class ImxActivityServiceTest {

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    private val expectedMintActivity by lazy {
        ImmutablexMintsPage(
            "", false,
            listOf(
                mapper.readValue(
                    ImxActivityServiceTest::class.java.getResourceAsStream("mint.json"),
                    ImmutablexMint::class.java
                )
            )
        )
    }

    private val expectedTransfersActivity by lazy {
        ImmutablexTransfersPage(
            "", false,
            listOf(
                mapper.readValue(
                    ImxActivityServiceTest::class.java.getResourceAsStream("transfer.json"),
                    ImmutablexTransfer::class.java
                )
            )
        )
    }

    private val expectedTradesActivity by lazy {
        ImmutablexTradesPage(
            "", false,
            listOf(
                mapper.readValue(
                    ImxActivityServiceTest::class.java.getResourceAsStream("trade.json"),
                    ImmutablexTrade::class.java
                )
            )
        )
    }

    private val service = ImxActivityService(
        mockk {
            coEvery {
                getMints(any(), any(), any(), any(), any(), any(), any(), any())
            } returns expectedMintActivity

            coEvery {
                getTransfers(any(), any(), any(), any(), any(), any(), any(), any())
            } returns expectedTransfersActivity

            coEvery {
                getTrades(any(), any(), any(), any(), any(), any(), any(), any())
            } returns expectedTradesActivity

        },
        ImxOrderService(
            orderClient = mockk {
                coEvery { getById(any()) } answers {
                    mapper.readValue(
                        ImxActivityServiceTest::class.java.getResourceAsStream("order.json"),
                        ImmutablexOrder::class.java
                    ).copy(orderId = it.invocation.args[0].toString().toLong())
                }
                coEvery { getByIds(listOf("28307", "28308")) } answers { ans ->
                    val orderIds = ans.invocation.args[0] as Collection<String>
                    orderIds.map {
                        mapper.readValue(
                            ImxActivityServiceTest::class.java.getResourceAsStream("order.json"),
                            ImmutablexOrder::class.java
                        ).copy(orderId = it.toLong())
                    }
                }

            }
        )

    )

    @Test
    fun getAllActivities() = runBlocking {
        service.getAllActivities(
            listOf(ActivityTypeDto.MINT, ActivityTypeDto.TRANSFER, ActivityTypeDto.SELL),
            null,
            50,
            null
        ).let { page ->
            assertEquals(page.entities.size, 3)
            page.entities.any { it is MintActivityDto }
            assert(page.entities.any { it is MintActivityDto })
            assert(page.entities.any { it is OrderMatchSellDto })
            assert(page.entities.any { it is TransferActivityDto })
        }

        service.getAllActivities(listOf(ActivityTypeDto.MINT), null, 50, null)
            .let { page ->
                assertEquals(page.entities.size, 1)
                val activity = page.entities.single() as MintActivityDto
                val expected = expectedMintActivity.result.single()
                assertEquals(activity.itemId!!.value, expected.token.data.itemId())
                assertEquals(activity.owner.value, expected.user)
            }

        service.getAllActivities(listOf(ActivityTypeDto.SELL), null, 50, null)
            .let { page ->
                assertEquals(page.entities.size, 1)
                val activity = page.entities.single() as OrderMatchSellDto
                val expected = expectedTradesActivity.result.single()
                assertEquals(activity.sellerOrderHash, expected.make.orderId.toString())
                assertEquals(activity.buyerOrderHash, expected.take.orderId.toString())
            }

        service.getAllActivities(listOf(ActivityTypeDto.TRANSFER), null, 50, null)
            .let { page ->
                assertEquals(page.entities.size, 1)
                val activity = page.entities.single() as TransferActivityDto
                val expected = expectedTransfersActivity.result.single()
                assertEquals(activity.itemId!!.value, expected.token.data.itemId())
                assertEquals(activity.value, expected.token.data.quantity)
                assertEquals(activity.from.value, expected.user)
                assertEquals(activity.owner.value, expected.receiver)
            }
    }

    @Test
    fun getActivitiesByItem() = runBlocking {
        val itemId = expectedMintActivity.result.single().token.data.itemId()

        service.getActivitiesByItem(
            listOf(ActivityTypeDto.MINT),
            itemId,
            null,
            50,
            null
        ).let { page ->
            println(page.entities)
            assertEquals(page.entities.size, 1)
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
            assertEquals(page.entities.size, 1)
            assert(page.entities[0] is MintActivityDto)
        }
    }

    @Test
    fun getActivitiesByItemAndOwner() = runBlocking {
        val (itemId, owner) = expectedMintActivity.result.single().run {
            token.data.itemId() to user
        }

        service.getActivitiesByItemAndOwner(
            types = listOf(ItemAndOwnerActivityType.MINT),
            itemId = itemId,
            owner = owner,
            null,
            50,
            null
        ).let { page ->
            assertEquals(1, page.entities.size)
            assert(page.entities[0] is MintActivityDto)
        }
    }
}
