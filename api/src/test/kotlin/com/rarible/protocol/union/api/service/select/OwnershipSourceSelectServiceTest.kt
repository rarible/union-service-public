package com.rarible.protocol.union.api.service.select

import com.rarible.protocol.union.api.service.api.OwnershipApiQueryService
import com.rarible.protocol.union.api.service.elastic.OwnershipElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.test.data.randomUnionAddress
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class OwnershipSourceSelectServiceTest {

    @MockK
    private lateinit var featureFlagsProperties: FeatureFlagsProperties

    @MockK
    private lateinit var ownershipApiQueryService: OwnershipApiQueryService

    @MockK
    private lateinit var ownershipElasticService: OwnershipElasticService

    @InjectMockKs
    private lateinit var service: OwnershipSourceSelectService

    @Test
    fun `should getOwnershipByOwner - api merge`() = runBlocking<Unit> {
        // given
        val owner = randomUnionAddress()
        val continuation = "123"
        val size = 10

        val expected = mockk< Slice<UnionOwnership>>()
        every { featureFlagsProperties.enableOwnershipQueriesToElasticSearch } returns false
        coEvery { ownershipApiQueryService.getOwnershipByOwner(owner, continuation, size) } returns expected

        // when
        val actual = service.getOwnershipByOwner(owner, continuation, size)

        // then
        assertThat(actual).isEqualTo(expected)
        coVerify { ownershipApiQueryService.getOwnershipByOwner(owner, continuation, size) }
        confirmVerified(ownershipApiQueryService, ownershipElasticService)
    }

    @Test
    fun `should getOwnershipByOwner - elastic`() = runBlocking<Unit> {
        // given
        val owner = randomUnionAddress()
        val continuation = "123"
        val size = 10

        val expected = mockk< Slice<UnionOwnership>>()
        every { featureFlagsProperties.enableOwnershipQueriesToElasticSearch } returns true
        coEvery { ownershipElasticService.getOwnershipByOwner(owner, continuation, size) } returns expected

        // when
        val actual = service.getOwnershipByOwner(owner, continuation, size)

        // then
        assertThat(actual).isEqualTo(expected)
        coVerify { ownershipElasticService.getOwnershipByOwner(owner, continuation, size) }
        confirmVerified(ownershipApiQueryService, ownershipElasticService)
    }

    @Test
    fun `should getOwnershipsByItem - api merge`() = runBlocking<Unit> {
        // given
        val item = randomEthItemId()
        val continuation = "123"
        val size = 10

        val expected = mockk<OwnershipsDto>()
        every { featureFlagsProperties.enableOwnershipQueriesToElasticSearch } returns false
        coEvery { ownershipApiQueryService.getOwnershipsByItem(item, continuation, size) } returns expected

        // when
        val actual = service.getOwnershipsByItem(item, continuation, size)

        // then
        assertThat(actual).isEqualTo(expected)
        coVerify { ownershipApiQueryService.getOwnershipsByItem(item, continuation, size) }
        confirmVerified(ownershipApiQueryService, ownershipElasticService)
    }

    @Test
    fun `should getOwnershipsByItem - elastic`() = runBlocking<Unit> {
        // given
        val item = randomEthItemId()
        val continuation = "123"
        val size = 10

        val expected = mockk<OwnershipsDto>()
        every { featureFlagsProperties.enableOwnershipQueriesToElasticSearch } returns true
        coEvery { ownershipElasticService.getOwnershipsByItem(item, continuation, size) } returns expected

        // when
        val actual = service.getOwnershipsByItem(item, continuation, size)

        // then
        assertThat(actual).isEqualTo(expected)
        coVerify { ownershipElasticService.getOwnershipsByItem(item, continuation, size) }
        confirmVerified(ownershipApiQueryService, ownershipElasticService)
    }
}
