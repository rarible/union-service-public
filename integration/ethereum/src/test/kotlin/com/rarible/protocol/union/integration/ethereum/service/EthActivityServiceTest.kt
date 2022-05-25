package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.ActivitiesByIdRequestDto
import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.AuctionActivitiesDto
import com.rarible.protocol.dto.AuctionActivityDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.dto.NftActivityFilterByItemAndOwnerDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.order.api.client.AuctionActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.core.model.ItemAndOwnerActivityType
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.ethereum.converter.EthActivityConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemMintActivity
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemTransferActivity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Mono
import java.time.Instant

@ExtendWith(MockKExtension::class)
internal class EthActivityServiceTest {

    @MockK
    private lateinit var activityItemControllerApi: NftActivityControllerApi

    @MockK
    private lateinit var activityOrderControllerApi: OrderActivityControllerApi

    @MockK
    private lateinit var activityAuctionControllerApi: AuctionActivityControllerApi

    @MockK
    private lateinit var ethActivityConverter: EthActivityConverter

    private var blockchainDto: BlockchainDto = BlockchainDto.ETHEREUM

    @InjectMockKs
    private lateinit var service: EthActivityService

    @Nested
    inner class GetActivitiesByItemAndOwnerTest {
        @Test
        fun `should get activities by item and owner`() = runBlocking<Unit> {
            val token = randomAddress()
            val tokenId = randomBigInt()
            val owner = randomAddress()
            val types = NftActivityFilterByItemAndOwnerDto.Types.values().asList()


            val filter = NftActivityFilterByItemAndOwnerDto(token, tokenId, owner, types)
            val ethMint = randomEthItemMintActivity().copy(owner = owner, contract = token, tokenId = tokenId)
            val ethTransfer = randomEthItemTransferActivity().copy(owner = owner, contract = token, tokenId = tokenId)
            val response = NftActivitiesDto(null, listOf(ethMint, ethTransfer))

            every {
                activityItemControllerApi.getNftActivities(filter, null, 10, ActivitySortDto.LATEST_FIRST)
            } returns Mono.just(response)

            val dateMint = Instant.now().minusSeconds(5)
            val dateTransfer = dateMint.plusSeconds(3)
            val unionMint = mockk<ActivityDto> {
                every { id } returns ActivityIdDto(blockchainDto, "A")
                every { date } returns dateMint
            }
            val unionTransfer = mockk<ActivityDto> {
                every { id } returns ActivityIdDto(blockchainDto, "B")
                every { date } returns dateTransfer
            }

            every { ethActivityConverter.convertToNftItemAndOwnerTypes(any()) } returns types
            coEvery { ethActivityConverter.convert(ethMint, blockchainDto) } returns unionMint
            coEvery { ethActivityConverter.convert(ethTransfer, blockchainDto) } returns unionTransfer

            val fullItemId = ItemIdDto(BlockchainDto.ETHEREUM, token.toString(), tokenId).toString()
            val itemId = IdParser.parseItemId(fullItemId).value

            val actual = service.getActivitiesByItemAndOwner(
                types = listOf(ItemAndOwnerActivityType.MINT, ItemAndOwnerActivityType.TRANSFER),
                itemId = itemId,
                owner = owner.toString(),
                continuation = null,
                size = 10,
                sort = com.rarible.protocol.union.dto.ActivitySortDto.LATEST_FIRST
            )

            assertThat(actual.entities).containsExactlyInAnyOrder(unionMint, unionTransfer)
        }
    }

    @Nested
    inner class GetActivitiesByIdsTest {

        @Test
        fun `should get activities by ids - happy path`() = runBlocking<Unit> {
            // given
            val typedIds = listOf(
                TypedActivityId("ETHEREUM:A", ActivityTypeDto.MINT),
                TypedActivityId("ETHEREUM:B", ActivityTypeDto.SELL),
                TypedActivityId("ETHEREUM:C", ActivityTypeDto.AUCTION_STARTED),
                TypedActivityId("ETHEREUM:D", ActivityTypeDto.BURN),
                TypedActivityId("ETHEREUM:E", ActivityTypeDto.BID),
                TypedActivityId("ETHEREUM:F", ActivityTypeDto.AUCTION_ENDED),
            )
            val itemActivityA = mockk<NftActivityDto>()
            val itemActivityD = mockk<NftActivityDto>()
            val itemActivityResp = NftActivitiesDto(null, listOf(itemActivityA, itemActivityD))
            every {
                activityItemControllerApi.getNftActivitiesById(ActivitiesByIdRequestDto(listOf("ETHEREUM:A", "ETHEREUM:D")))
            } returns Mono.just(itemActivityResp)

            val orderActivityB = mockk<OrderActivityDto>()
            val orderActivityE = mockk<OrderActivityDto>()
            val orderActivityResp = OrderActivitiesDto(null, listOf(orderActivityB, orderActivityE))
            every {
                activityOrderControllerApi.getOrderActivitiesById(ActivitiesByIdRequestDto(listOf("ETHEREUM:B", "ETHEREUM:E")))
            } returns Mono.just(orderActivityResp)

            val auctionActivityC = mockk<AuctionActivityDto>()
            val auctionActivityF = mockk<AuctionActivityDto>()
            val auctionActivityResp = AuctionActivitiesDto(null, listOf(auctionActivityC, auctionActivityF))
            every {
                activityAuctionControllerApi.getAuctionActivitiesById(ActivitiesByIdRequestDto(listOf("ETHEREUM:C", "ETHEREUM:F")))
            } returns Mono.just(auctionActivityResp)

            val unionActivityA = mockk<ActivityDto> {
                every { id } returns ActivityIdDto(BlockchainDto.ETHEREUM, "A")
            }
            val unionActivityB = mockk<ActivityDto> {
                every { id } returns ActivityIdDto(BlockchainDto.ETHEREUM, "B")
            }
            val unionActivityC = mockk<ActivityDto> {
                every { id } returns ActivityIdDto(BlockchainDto.ETHEREUM, "C")
            }
            val unionActivityD = mockk<ActivityDto> {
                every { id } returns ActivityIdDto(BlockchainDto.ETHEREUM, "D")
            }
            val unionActivityE = mockk<ActivityDto> {
                every { id } returns ActivityIdDto(BlockchainDto.ETHEREUM, "E")
            }
            val unionActivityF = mockk<ActivityDto> {
                every { id } returns ActivityIdDto(BlockchainDto.ETHEREUM, "F")
            }
            coEvery { ethActivityConverter.convert(itemActivityA, blockchainDto) } returns unionActivityA
            coEvery { ethActivityConverter.convert(orderActivityB, blockchainDto) } returns unionActivityB
            coEvery { ethActivityConverter.convert(auctionActivityC, blockchainDto) } returns unionActivityC
            coEvery { ethActivityConverter.convert(itemActivityD, blockchainDto) } returns unionActivityD
            coEvery { ethActivityConverter.convert(orderActivityE, blockchainDto) } returns unionActivityE
            coEvery { ethActivityConverter.convert(auctionActivityF, blockchainDto) } returns unionActivityF

            // when
            val actual = service.getActivitiesByIds(typedIds)

            // then
            assertThat(actual).containsExactlyInAnyOrder(unionActivityA, unionActivityB, unionActivityC, unionActivityD, unionActivityE, unionActivityF)
        }
    }
}
