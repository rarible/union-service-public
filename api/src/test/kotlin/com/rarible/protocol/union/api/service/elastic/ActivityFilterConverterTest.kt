package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.UserActivityTypeConverter
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.ActivityByCollectionFilter
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.dto.CollectionIdDto
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant


@ExtendWith(MockKExtension::class)
class ActivityFilterConverterTest {

    @SpyK
    private var userActivityTypeConverter: UserActivityTypeConverter = UserActivityTypeConverter()

    @InjectMockKs
    private lateinit var converter: ActivityFilterConverter

    private val emptyGenericFilter = ElasticActivityQueryGenericFilter()

    @Nested
    inner class ConvertGetAllActivitiesTest {

        @Test
        fun `should convert - happy path`() {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.BURN)
            val blockchains = listOf(BlockchainDto.POLYGON, BlockchainDto.SOLANA)
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetAllActivities(types, blockchains, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("blockchains", "activityTypes", "cursor")
                .isEqualTo(emptyGenericFilter)
            actual as ElasticActivityQueryGenericFilter
            assertThat(actual.blockchains).containsExactlyInAnyOrder(*blockchains.toTypedArray())
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(*types.toTypedArray())
            assertThat(actual.cursor).isEqualTo(cursor)
        }

        @Test
        fun `should convert - null blockchains`() {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.BURN)
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetAllActivities(types, null, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "cursor")
                .isEqualTo(emptyGenericFilter)
            actual as ElasticActivityQueryGenericFilter
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(*types.toTypedArray())
            assertThat(actual.cursor).isEqualTo(cursor)
        }
    }

    @Nested
    inner class ConvertGetActivitiesByCollectionTest {

        @Test
        fun `should convert - happy path`() {
            // given
            val types = listOf(ActivityTypeDto.LIST, ActivityTypeDto.BID)
            val collection = "POLYGON:0x00000012345"
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetActivitiesByCollection(types, listOf(collection), cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "collections", "blockchains", "cursor")
                .isEqualTo(emptyGenericFilter)
            actual as ActivityByCollectionFilter
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(*types.toTypedArray())
            assertThat(actual.collections).containsExactly(CollectionIdDto(BlockchainDto.POLYGON, "0x00000012345"))
            assertThat(actual.cursor).isEqualTo(cursor)
        }
    }

    @Nested
    inner class ConvertGetActivitiesByItemTest {

        @Test
        fun `should convert - happy path`() {
            // given
            val types = listOf(ActivityTypeDto.CANCEL_LIST, ActivityTypeDto.CANCEL_BID)
            val itemId = "TEZOS:0x00000012345"
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetActivitiesByItem(types, itemId, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "item", "blockchains", "cursor")
                .isEqualTo(emptyGenericFilter)
            actual as ElasticActivityQueryGenericFilter
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(*types.toTypedArray())
            assertThat(actual.item).isEqualTo("0x00000012345")
            assertThat(actual.blockchains).containsExactly(BlockchainDto.TEZOS)
            assertThat(actual.cursor).isEqualTo(cursor)
        }
    }

    @Nested
    inner class ConvertGetActivitiesByUserTest {

        @Test
        fun `should convert - happy path`() {
            // given
            val userActivityTypes = listOf(UserActivityTypeDto.TRANSFER_FROM, UserActivityTypeDto.TRANSFER_TO, UserActivityTypeDto.BUY)
            val users = listOf("SOLANA:loupa", "FLOW:poupa")
            val blockchains = listOf(BlockchainDto.SOLANA, BlockchainDto.FLOW)
            val from = Instant.ofEpochMilli(12345)
            val to = Instant.ofEpochMilli(67890)
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetActivitiesByUser(userActivityTypes, users, blockchains, from, to, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "blockchains", "anyUsers", "from", "to", "cursor")
                .isEqualTo(emptyGenericFilter)
            actual as ElasticActivityQueryGenericFilter
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(ActivityTypeDto.TRANSFER, ActivityTypeDto.SELL)
            assertThat(actual.blockchains).containsExactlyInAnyOrder(*blockchains.toTypedArray())
            assertThat(actual.anyUsers).containsExactlyInAnyOrder("loupa", "poupa")
            assertThat(actual.usersFrom).isEmpty()
            assertThat(actual.usersTo).isEmpty()
            assertThat(actual.from).isEqualTo(from)
            assertThat(actual.to).isEqualTo(to)
            assertThat(actual.cursor).isEqualTo(cursor)
        }

        @Test
        fun `should convert - filter by from users only`() {
            // given
            val userActivityTypes = listOf(UserActivityTypeDto.TRANSFER_FROM, UserActivityTypeDto.SELL)
            val users = listOf("SOLANA:loupa", "FLOW:poupa")
            val blockchains = listOf(BlockchainDto.SOLANA, BlockchainDto.FLOW)
            val from = Instant.ofEpochMilli(12345)
            val to = Instant.ofEpochMilli(67890)
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetActivitiesByUser(userActivityTypes, users, blockchains, from, to, cursor)

            // then
            actual as ElasticActivityQueryGenericFilter
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(ActivityTypeDto.TRANSFER, ActivityTypeDto.SELL)
            assertThat(actual.blockchains).containsExactlyInAnyOrder(*blockchains.toTypedArray())
            assertThat(actual.anyUsers).isEmpty()
            assertThat(actual.usersFrom).containsExactlyInAnyOrder("loupa", "poupa")
            assertThat(actual.from).isEqualTo(from)
            assertThat(actual.to).isEqualTo(to)
            assertThat(actual.cursor).isEqualTo(cursor)
        }

        @Test
        fun `should convert - filter by to users only`() {
            // given
            val userActivityTypes = listOf(UserActivityTypeDto.TRANSFER_TO, UserActivityTypeDto.BUY)
            val users = listOf("SOLANA:loupa", "FLOW:poupa")
            val blockchains = listOf(BlockchainDto.SOLANA, BlockchainDto.FLOW)
            val from = Instant.ofEpochMilli(12345)
            val to = Instant.ofEpochMilli(67890)
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetActivitiesByUser(userActivityTypes, users, blockchains, from, to, cursor)

            // then
            actual as ElasticActivityQueryGenericFilter
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(ActivityTypeDto.TRANSFER, ActivityTypeDto.SELL)
            assertThat(actual.blockchains).containsExactlyInAnyOrder(*blockchains.toTypedArray())
            assertThat(actual.anyUsers).isEmpty()
            assertThat(actual.usersTo).containsExactlyInAnyOrder("loupa", "poupa")
            assertThat(actual.from).isEqualTo(from)
            assertThat(actual.to).isEqualTo(to)
            assertThat(actual.cursor).isEqualTo(cursor)
        }

        @Test
        fun `should convert - null blockchains`() {
            // given
            val userActivityTypes = listOf(UserActivityTypeDto.TRANSFER_FROM, UserActivityTypeDto.TRANSFER_TO, UserActivityTypeDto.BUY)
            val users = listOf("SOLANA:loupa", "FLOW:poupa")
            val from = Instant.ofEpochMilli(12345)
            val to = Instant.ofEpochMilli(67890)
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetActivitiesByUser(userActivityTypes, users, null, from, to, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "anyUsers", "from", "to", "cursor")
                .isEqualTo(emptyGenericFilter)
        }
    }
}
