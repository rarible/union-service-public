package com.rarible.protocol.union.api.service

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.api.service.elastic.OwnershipElasticHelper
import com.rarible.protocol.union.api.service.elastic.OwnershipElasticService
import com.rarible.protocol.union.core.DefaultBlockchainProperties
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.core.service.AuctionContractService
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentAuctionService
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.enrichment.service.query.order.OrderApiService
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOwnershipConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthAuctionDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipDto
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@ExperimentalCoroutinesApi
class OwnershipElasticServiceTest {

    private val auctionContract = randomAddress()

    private val properties = DefaultBlockchainProperties(
        blockchain = BlockchainDto.ETHEREUM,
        enabled = true,
        consumer = null,
        client = null,
        auctionContracts = auctionContract.prefixed()
    )

    private val orderApiService = mockk<OrderApiService>()
    private val auctionContractService: AuctionContractService = AuctionContractService(listOf(properties))
    private val enrichmentOwnershipService: EnrichmentOwnershipService = mockk()
    private val enrichmentAuctionService: EnrichmentAuctionService = mockk()
    private val repository = mockk<EsOwnershipRepository>()
    private val elasticHelper = mockk<OwnershipElasticHelper>()

    private val ethAuctionConverter = EthAuctionConverter(CurrencyMock.currencyServiceMock)

    private val apiHelper: EnrichedOwnershipApiHelper = EnrichedOwnershipApiHelper(
        orderApiService,
        auctionContractService,
        enrichmentOwnershipService,
        enrichmentAuctionService,
        mockk(),
    )

    private val ownershipElasticService = OwnershipElasticService(
        enrichmentAuctionService,
        apiHelper,
        elasticHelper,
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(repository, enrichmentOwnershipService, enrichmentAuctionService, orderApiService)
        coEvery { enrichmentOwnershipService.findAll(any()) } returns emptyList()
        coEvery { enrichmentOwnershipService.mergeWithAuction(any<OwnershipDto>(), any()) } returnsArgument 0
        coEvery { enrichmentOwnershipService.mergeWithAuction(any<UnionOwnership>(), any()) } returnsArgument 0
        coEvery { orderApiService.getByIds(any<List<OrderIdDto>>()) } returns emptyList()
        coEvery { repository.findByFilter(any()) } returns emptyList()
    }

    @Test
    fun `first page with auctions - full auction in page`() = runBlocking<Unit> {
        val now = nowMillis()
        val itemId = randomEthItemId()

        val fullAuction = auction(itemId)
            .copy(createdAt = now.minusSeconds(1))
        val fullAuctionOwnership = ownership(itemId, fullAuction.contract.value)
        val disguisedFullAuctionOwnership = fullAuctionOwnership.copy(
            id = fullAuctionOwnership.id.copy(owner = fullAuction.seller),
            createdAt = fullAuction.createdAt
        )

        val partialAuction = auction(itemId)
            .copy(createdAt = now.plusSeconds(4))

        mockAuctions(itemId, fullAuction, partialAuction)
        mockDisguise(fullAuction, disguisedFullAuctionOwnership)

        val freeOwnership = ownership(itemId)
            .copy(createdAt = now.minusSeconds(3))
        val partialOwnership = ownership(itemId, partialAuction.seller.value)
            .copy(createdAt = now.minusSeconds(2))

        mockOwnershipsNotFound(fullAuction.getSellerOwnershipId())
        mockOwnerships(partialOwnership)

        // 3 ownerships should be requested since there is 1 full auction found
        mockItemOwnershipsQuery(
            itemId, null, 3,
            freeOwnership, // should be trimmed due to requested page size is 2
            partialOwnership, // should be extended
            fullAuctionOwnership // should be disguised
        )

        val result = ownershipElasticService.getOwnershipsByItem(itemId, null, 2).ownerships

        // Full auction ownership - the earliest
        assertThat(result[0].id).isEqualTo(fullAuctionOwnership.id.copy(owner = fullAuction.seller))
        // Partial auction ownership - second
        assertThat(result[1].id).isEqualTo(partialOwnership.id)
    }

    @Test
    fun `second page with auctions - full auction filtered`() = runBlocking<Unit> {
        val now = nowMillis()
        val itemId = randomEthItemId()

        val fullAuction = auction(itemId)
            .copy(createdAt = now.minusSeconds(1))
        val fullAuctionOwnership = ownership(itemId, fullAuction.contract.value)

        val partialAuction = auction(itemId)
            .copy(createdAt = now.plusSeconds(4))

        mockAuctions(itemId, fullAuction, partialAuction)

        val freeOwnership = ownership(itemId)
            .copy(createdAt = now.minusSeconds(3))
        val partialOwnership = ownership(itemId, partialAuction.seller.value)
            .copy(createdAt = now.minusSeconds(2))

        mockOwnershipsNotFound(fullAuction.getSellerOwnershipId())
        mockOwnerships(partialOwnership)

        val continuation = DateIdContinuation(
            fullAuction.createdAt.plusSeconds(1), fullAuctionOwnership.id.value
        ).toString()

        // 2 ownerships should be requested since full auction was filtered
        mockItemOwnershipsQuery(
            itemId, continuation, 2,
            freeOwnership,
            partialOwnership,
        )

        val result = ownershipElasticService.getOwnershipsByItem(itemId, continuation, 2).ownerships

        // Partial auction ownership - first
        assertThat(result[0].id).isEqualTo(partialOwnership.id)
        // Free - second, full auction filtered by continuation
        assertThat(result[1].id).isEqualTo(freeOwnership.id)
    }

    private suspend fun mockItemOwnershipsQuery(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int,
        vararg ownerships: UnionOwnership,
    ) {
        coEvery {
            elasticHelper.getRawOwnershipsByItem(itemId, continuation, size)
        } returns ownerships.asList()
    }

    private suspend fun mockOwnerships(vararg ownerships: UnionOwnership) {
        ownerships.forEach {
            coEvery { enrichmentOwnershipService.fetchOrNull(ShortOwnershipId(it.id)) } returns it
        }
    }

    private suspend fun mockOwnershipsNotFound(vararg ids: OwnershipIdDto) {
        ids.forEach {
            coEvery { enrichmentOwnershipService.fetchOrNull(ShortOwnershipId(it)) } returns null
        }
    }

    private suspend fun mockDisguise(auction: AuctionDto, ownership: UnionOwnership) {
        coEvery {
            enrichmentOwnershipService.disguiseAuctionWithEnrichment(auction)
        } returns EnrichedOwnershipConverter.convert(ownership)
    }

    private suspend fun mockAuctions(itemId: ItemIdDto, vararg auctions: AuctionDto) {
        coEvery {
            enrichmentAuctionService.findByItem(ShortItemId(itemId))
        } returns auctions.asList()
    }

    private suspend fun auction(itemId: ItemIdDto): AuctionDto {
        return ethAuctionConverter
            .convert(randomEthAuctionDto(itemId), BlockchainDto.ETHEREUM)
            .copy(contract = ContractAddress(BlockchainDto.ETHEREUM, auctionContract.prefixed()))
    }

    private fun ownership(itemId: ItemIdDto): UnionOwnership {
        return ownership(itemId, randomAddress().prefixed())
    }

    private fun ownership(itemId: ItemIdDto, owner: String): UnionOwnership {
        return EthOwnershipConverter.convert(
            randomEthOwnershipDto(itemId.toOwnership(owner)),
            BlockchainDto.ETHEREUM
        )
    }

}
