package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthCollectionAssetType
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.ActivityDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentActivityData
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolver
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityOrderBid
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityOrderList
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityTransfer
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EnrichmentActivityServiceTest {

    private val typesMint = listOf(ActivityTypeDto.MINT)
    private val typesTransfer = listOf(ActivityTypeDto.TRANSFER)

    private val blockchain = BlockchainDto.ETHEREUM
    private val activityService: ActivityService = mockk()
    private val customCollectionResolver: CustomCollectionResolver = mockk()
    private val activityRepository: ActivityRepository = mockk()

    private lateinit var service: EnrichmentActivityService

    @BeforeEach
    fun beforeEach() {
        clearMocks(activityService)
        every { activityService.blockchain } returns blockchain
        service = EnrichmentActivityService(
            BlockchainRouter(listOf(activityService), listOf(blockchain)),
            customCollectionResolver,
            activityRepository,
            FeatureFlagsProperties()
        )
    }

    @Test
    fun `enrich - ok`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityMint(itemId)

        coEvery { customCollectionResolver.resolveCustomCollection(itemId) } returns null

        val result = service.enrich(activity)
        val expected = EnrichmentActivityConverter.convert(activity)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `enrich - ok deprecated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityMint(itemId)

        coEvery { customCollectionResolver.resolveCustomCollection(itemId) } returns null

        val result = service.enrichDeprecated(activity)
        val expected = ActivityDtoConverter.convert(activity)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `enrich - ok, custom collection by item deprecated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val customCollection = randomEthCollectionId()
        val activity = randomUnionActivityMint(itemId)

        coEvery { customCollectionResolver.resolveCustomCollection(itemId) } returns customCollection

        val result = service.enrichDeprecated(activity)
        val expected = (ActivityDtoConverter.convert(activity) as MintActivityDto)
            .copy(collection = customCollection)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `enrich - ok, custom collection by item`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val customCollection = randomEthCollectionId()
        val activity = randomUnionActivityMint(itemId)

        coEvery { customCollectionResolver.resolveCustomCollection(itemId) } returns customCollection

        val result = service.enrich(activity)
        val expected =
            EnrichmentActivityConverter.convert(activity, EnrichmentActivityData(customCollection = customCollection))

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `enrich - ok, custom collection by collection deprecated`() = runBlocking<Unit> {
        val contract = ContractAddress(BlockchainDto.ETHEREUM, randomEthAddress())
        val collectionId = CollectionIdDto(contract.blockchain, contract.value)
        val collectionAsset = UnionAsset(UnionEthCollectionAssetType(contract), BigDecimal.ONE)
        val customCollection = randomEthCollectionId()

        val activity = randomUnionActivityOrderList(BlockchainDto.ETHEREUM)
            .copy(make = collectionAsset)

        coEvery { customCollectionResolver.resolveCustomCollection(collectionId) } returns customCollection

        val result = service.enrichDeprecated(activity)
        val expected = (ActivityDtoConverter.convert(activity) as OrderListActivityDto)
            .copy(make = AssetDto(EthCollectionAssetTypeDto(contract, customCollection), BigDecimal.ONE))

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `enrich - ok, custom collection by collection`() = runBlocking<Unit> {
        val contract = ContractAddress(BlockchainDto.ETHEREUM, randomEthAddress())
        val collectionId = CollectionIdDto(contract.blockchain, contract.value)
        val collectionAsset = UnionAsset(UnionEthCollectionAssetType(contract), BigDecimal.ONE)
        val customCollection = randomEthCollectionId()

        val activity = randomUnionActivityOrderBid(BlockchainDto.ETHEREUM)
            .copy(take = collectionAsset)

        coEvery { customCollectionResolver.resolveCustomCollection(collectionId) } returns customCollection

        val result = service.enrich(activity)
        val expected = EnrichmentActivityConverter.convert(
            activity,
            EnrichmentActivityData(customCollection = customCollection)
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `mint source deprecated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityMint(itemId)

        // Mint found, owner matches
        coEvery {
            activityService.getActivitiesByItem(typesMint, itemId.value, null, 1, ActivitySortDto.LATEST_FIRST)
        } returns Slice(null, listOf(activity))

        val source = service.getOwnershipSource(itemId.toOwnership(activity.owner.value))

        assertThat(source).isEqualTo(OwnershipSourceDto.MINT)
    }

    @Test
    fun `mint source`() = runBlocking<Unit> {
        service = EnrichmentActivityService(
            BlockchainRouter(listOf(activityService), listOf(blockchain)),
            customCollectionResolver,
            activityRepository,
            FeatureFlagsProperties(enableMongoActivityRead = true)
        )
        val itemId = randomEthItemId()
        val activity = randomUnionActivityMint(itemId)

        // Mint found, owner matches
        coEvery {
            activityRepository.isMinter(itemId, activity.owner)
        } returns true

        val source = service.getOwnershipSource(itemId.toOwnership(activity.owner.value))

        assertThat(source).isEqualTo(OwnershipSourceDto.MINT)
    }

    @Test
    fun `purchase source deprecated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityTransfer(itemId).copy(purchase = true)
        val otherOwnerTransfer = randomUnionActivityTransfer(itemId)
        val otherOwnerMint = randomUnionActivityMint(itemId)

        // Mint found, but owner is different
        coEvery {
            activityService.getActivitiesByItem(typesMint, any(), any(), any(), any())
        } returns Slice(null, listOf(otherOwnerMint))

        // Transfer found for target owner
        coEvery {
            activityService.getActivitiesByItem(typesTransfer, itemId.value, null, 100, ActivitySortDto.LATEST_FIRST)
        } returns Slice(null, listOf(otherOwnerTransfer, activity))

        val source = service.getOwnershipSource(itemId.toOwnership(activity.owner.value))

        assertThat(source).isEqualTo(OwnershipSourceDto.PURCHASE)
    }

    @Test
    fun `purchase source`() = runBlocking<Unit> {
        service = EnrichmentActivityService(
            BlockchainRouter(listOf(activityService), listOf(blockchain)),
            customCollectionResolver,
            activityRepository,
            FeatureFlagsProperties(enableMongoActivityRead = true)
        )
        val itemId = randomEthItemId()
        val activity = randomUnionActivityTransfer(itemId).copy(purchase = true)

        coEvery {
            activityRepository.isMinter(itemId, activity.owner)
        } returns false
        coEvery {
            activityRepository.isBuyer(itemId, activity.owner)
        } returns true

        val source = service.getOwnershipSource(itemId.toOwnership(activity.owner.value))

        assertThat(source).isEqualTo(OwnershipSourceDto.PURCHASE)
    }

    @Test
    fun `transfer source - found deprecated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityTransfer(itemId).copy(purchase = false)

        // Mint not found
        coEvery {
            activityService.getActivitiesByItem(listOf(ActivityTypeDto.MINT), any(), any(), any(), any())
        } returns Slice.empty()

        // Transfer found, purchase = false
        coEvery {
            activityService.getActivitiesByItem(typesTransfer, itemId.value, null, 100, ActivitySortDto.LATEST_FIRST)
        } returns Slice(null, listOf(activity))

        val source = service.getOwnershipSource(itemId.toOwnership(activity.owner.value))

        assertThat(source).isEqualTo(OwnershipSourceDto.TRANSFER)
    }

    @Test
    fun `transfer type - nothing found deprecated`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        // Mint not found
        coEvery {
            activityService.getActivitiesByItem(listOf(ActivityTypeDto.MINT), any(), any(), any(), any())
        } returns Slice.empty()

        // Transfer not found
        coEvery {
            activityService.getActivitiesByItem(typesTransfer, itemId.value, null, 100, ActivitySortDto.LATEST_FIRST)
        } returns Slice.empty()

        val source = service.getOwnershipSource(randomEthOwnershipId(itemId))

        assertThat(source).isEqualTo(OwnershipSourceDto.TRANSFER)
    }

    @Test
    fun `transfer type - nothing found`() = runBlocking<Unit> {
        service = EnrichmentActivityService(
            BlockchainRouter(listOf(activityService), listOf(blockchain)),
            customCollectionResolver,
            activityRepository,
            FeatureFlagsProperties(enableMongoActivityRead = true)
        )
        val itemId = randomEthItemId()
        val ownershipId = randomEthOwnershipId(itemId)

        coEvery {
            activityRepository.isMinter(itemId, ownershipId.owner)
        } returns false
        coEvery {
            activityRepository.isBuyer(itemId, ownershipId.owner)
        } returns false

        val source = service.getOwnershipSource(ownershipId)

        assertThat(source).isEqualTo(OwnershipSourceDto.TRANSFER)
    }
}