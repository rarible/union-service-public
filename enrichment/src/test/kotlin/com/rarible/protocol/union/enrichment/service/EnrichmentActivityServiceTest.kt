package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthCollectionAssetType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentActivityData
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolutionRequest
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolver
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityOrderBid
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityTransfer
import com.rarible.protocol.union.integration.ethereum.data.randomEthAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class EnrichmentActivityServiceTest {

    @MockK
    private lateinit var customCollectionResolver: CustomCollectionResolver

    @MockK
    private lateinit var activityRepository: ActivityRepository

    @InjectMockKs
    private lateinit var service: EnrichmentActivityService

    @Test
    fun `enrich - ok`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityMint(itemId)

        mockCustomCollection(activity, itemId, activity.collection, null)

        val result = service.enrich(activity)
        val expected = EnrichmentActivityConverter.convert(activity)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `enrich - ok, custom collection by item`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val customCollection = randomEthCollectionId()
        val activity = randomUnionActivityMint(itemId)

        mockCustomCollection(activity, itemId, activity.collection, customCollection)

        val result = service.enrich(activity)
        val expected = EnrichmentActivityConverter.convert(
            activity,
            EnrichmentActivityData(mapOf(activity.id to customCollection))
        )

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

        mockCustomCollection(activity, null, collectionId, customCollection)

        val result = service.enrich(activity)
        val expected = EnrichmentActivityConverter.convert(
            activity,
            EnrichmentActivityData(mapOf(activity.id to customCollection))
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `mint source`() = runBlocking<Unit> {
        service = EnrichmentActivityService(
            customCollectionResolver,
            activityRepository,
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
    fun `purchase source`() = runBlocking<Unit> {
        service = EnrichmentActivityService(
            customCollectionResolver,
            activityRepository,
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
    fun `transfer type - nothing found`() = runBlocking<Unit> {
        service = EnrichmentActivityService(
            customCollectionResolver,
            activityRepository,
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

    private fun mockCustomCollection(
        activity: UnionActivity,
        itemId: ItemIdDto? = null,
        collectionId: CollectionIdDto? = null,
        result: CollectionIdDto?
    ) {
        coEvery {
            customCollectionResolver.resolve(
                listOf(
                    CustomCollectionResolutionRequest(
                        entityId = activity.id,
                        itemId = itemId,
                        collectionId = collectionId
                    )
                ),
                emptyMap()
            )
        } returns (result?.let { mapOf(activity.id to result) } ?: emptyMap())
    }
}
