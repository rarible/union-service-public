package com.rarible.protocol.union.api.service.select

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.service.api.OwnershipApiService
import com.rarible.protocol.union.api.service.elastic.OwnershipElasticService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.test.data.randomUnionAddress
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

@ExtendWith(MockKExtension::class)
class OwnershipSelectServiceTest {

    @MockK
    private lateinit var featureFlagsProperties: FeatureFlagsProperties

    @MockK
    private lateinit var apiService: OwnershipApiService

    @MockK
    private lateinit var elasticService: OwnershipElasticService

    @InjectMockKs
    private lateinit var service: OwnershipSourceSelectService


    @Nested
    inner class GetOwnershipByIdTest {
        @Test
        fun `should get ownership by id - select elastic`() = runBlocking<Unit> {
            // given
            val id = mockk<OwnershipIdDto>()
            val response = mockk<OwnershipDto>()

            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery { elasticService.getOwnershipById(id) } returns response

            // when
            val actual = service.getOwnershipById(id)

            // then
            assertThat(actual).isEqualTo(response)
        }

        @Test
        fun `should get ownership by id - select api`() = runBlocking<Unit> {
            // given
            val id = mockk<OwnershipIdDto>()
            val response = mockk<OwnershipDto>()

            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery { apiService.getOwnershipById(id) } returns response

            // when
            val actual = service.getOwnershipById(id)

            // then
            assertThat(actual).isEqualTo(response)
        }
    }

    @Nested
    inner class GetOwnershipByOwnerTest {
        @Test
        fun `should get ownerships by owner - select elastic`() = runBlocking<Unit> {
            // given
            val owner = randomUnionAddress()
            val continuation = randomString()
            val size = randomInt()
            val response = mockk<Slice<UnionOwnership>>()

            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery { elasticService.getOwnershipByOwner(owner, continuation, size) } returns response

            // when
            val actual = service.getOwnershipByOwner(owner, continuation, size)

            // then
            assertThat(actual).isEqualTo(response)
        }

        @Test
        fun `should get ownerships by owner - select api`() = runBlocking<Unit> {
            // given
            val owner = randomUnionAddress()
            val continuation = randomString()
            val size = randomInt()
            val response = mockk<Slice<UnionOwnership>>()

            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery { apiService.getOwnershipByOwner(owner, continuation, size) } returns response

            // when
            val actual = service.getOwnershipByOwner(owner, continuation, size)

            // then
            assertThat(actual).isEqualTo(response)
        }
    }

    @Nested
    inner class GetOwnershipsByItemTest {
        @Test
        fun `should get ownerships by item - select elastic`() = runBlocking<Unit> {
            // given
            val itemId = mockk<ItemIdDto>()
            val continuation = randomString()
            val size = randomInt()
            val response = mockk<OwnershipsDto>()

            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns true
            coEvery { elasticService.getOwnershipsByItem(itemId, continuation, size) } returns response

            // when
            val actual = service.getOwnershipsByItem(itemId, continuation, size)

            // then
            assertThat(actual).isEqualTo(response)
        }

        @Test
        fun `should get ownerships by item - select api`() = runBlocking<Unit> {
            // given
            val itemId = mockk<ItemIdDto>()
            val continuation = randomString()
            val size = randomInt()
            val response = mockk<OwnershipsDto>()

            every { featureFlagsProperties.enableOrderQueriesToElasticSearch } returns false
            coEvery { apiService.getOwnershipsByItem(itemId, continuation, size) } returns response

            // when
            val actual = service.getOwnershipsByItem(itemId, continuation, size)

            // then
            assertThat(actual).isEqualTo(response)
        }
    }
}
