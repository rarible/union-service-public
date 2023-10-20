package com.rarible.protocol.union.api.service.elastic

import com.rarible.protocol.union.api.service.UserActivityTypeConverter
import com.rarible.protocol.union.core.model.elastic.ElasticActivityFilter
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CurrencyIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import scalether.domain.AddressFactory
import java.time.Instant

@ExtendWith(MockKExtension::class)
class ActivityFilterConverterTest {

    @SpyK
    private var userActivityTypeConverter: UserActivityTypeConverter = UserActivityTypeConverter()

    @InjectMockKs
    private lateinit var converter: ActivityFilterConverter

    private val emptyGenericFilter = ElasticActivityFilter()

    @Nested
    inner class ConvertGetAllActivitiesTest {

        @Test
        fun `should convert - happy path`() {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.BURN)
            val blockchains = listOf(BlockchainDto.POLYGON, BlockchainDto.SOLANA)
            val bidCurrencies = listOf(
                CurrencyIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = AddressFactory.create().toString(),
                    tokenId = null
                )
            )
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetAllActivities(types, blockchains, bidCurrencies, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("blockchains", "activityTypes", "cursor", "bidCurrencies")
                .isEqualTo(emptyGenericFilter)
            assertThat(actual.blockchains).containsExactlyInAnyOrder(*blockchains.toTypedArray())
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(*types.toTypedArray())
            assertThat(actual.bidCurrencies).isEqualTo(bidCurrencies.toSet())
            assertThat(actual.cursor).isEqualTo(cursor)
        }

        @Test
        fun `should convert - null blockchains`() {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.BURN)
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetAllActivities(types, null, null, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "cursor")
                .isEqualTo(emptyGenericFilter)
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
            val bidCurrencies = listOf(
                CurrencyIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = AddressFactory.create().toString(),
                    tokenId = null
                )
            )

            // when
            val actual = converter.convertGetActivitiesByCollection(types, listOf(collection), bidCurrencies, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "collections", "blockchains", "cursor", "bidCurrencies")
                .isEqualTo(emptyGenericFilter)
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(*types.toTypedArray())
            assertThat(actual.collections).containsExactly(
                CollectionIdDto(
                    BlockchainDto.POLYGON,
                    "0x00000012345"
                )
            )
            assertThat(actual.cursor).isEqualTo(cursor)
            assertThat(actual.bidCurrencies).isEqualTo(bidCurrencies.toSet())
        }
    }

    @Nested
    inner class ConvertGetActivitiesByItemTest {

        @Test
        fun `should convert - happy path`() {
            // given
            val types = listOf(ActivityTypeDto.CANCEL_LIST, ActivityTypeDto.CANCEL_BID)
            val itemId = "TEZOS:0x00000012345:1"
            val cursor = "some cursor"
            val bidCurrencies = listOf(
                CurrencyIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = AddressFactory.create().toString(),
                    tokenId = null
                )
            )

            // when
            val actual = converter.convertGetActivitiesByItem(types, itemId, bidCurrencies, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "items", "blockchains", "cursor", "bidCurrencies")
                .isEqualTo(emptyGenericFilter)
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(*types.toTypedArray())
            assertThat(actual.items).isEqualTo(setOf(ItemIdDto(BlockchainDto.TEZOS, "0x00000012345:1")))
            assertThat(actual.blockchains).containsExactly(BlockchainDto.TEZOS)
            assertThat(actual.cursor).isEqualTo(cursor)
            assertThat(actual.bidCurrencies).isEqualTo(bidCurrencies.toSet())
        }
    }

    @Nested
    inner class ConvertGetActivitiesByUserTest {

        @Test
        fun `should convert - happy path`() {
            // given
            val userActivityTypes =
                listOf(UserActivityTypeDto.TRANSFER_FROM, UserActivityTypeDto.TRANSFER_TO, UserActivityTypeDto.BUY)
            val users = listOf("SOLANA:loupa", "FLOW:poupa")
            val blockchains = listOf(BlockchainDto.SOLANA, BlockchainDto.FLOW)
            val from = Instant.ofEpochMilli(12345)
            val to = Instant.ofEpochMilli(67890)
            val cursor = "some cursor"
            val bidCurrencies = listOf(
                CurrencyIdDto(
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = AddressFactory.create().toString(),
                    tokenId = null
                )
            )

            // when
            val actual = converter.convertGetActivitiesByUser(
                userActivityTypes,
                users,
                blockchains,
                bidCurrencies,
                from,
                to,
                cursor
            )

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "blockchains", "anyUsers", "from", "to", "cursor", "bidCurrencies")
                .isEqualTo(emptyGenericFilter)
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(ActivityTypeDto.TRANSFER, ActivityTypeDto.SELL)
            assertThat(actual.blockchains).containsExactlyInAnyOrder(*blockchains.toTypedArray())
            assertThat(actual.anyUsers).containsExactlyInAnyOrder("loupa", "poupa")
            assertThat(actual.usersFrom).isEmpty()
            assertThat(actual.usersTo).isEmpty()
            assertThat(actual.from).isEqualTo(from)
            assertThat(actual.to).isEqualTo(to)
            assertThat(actual.cursor).isEqualTo(cursor)
            assertThat(actual.bidCurrencies).isEqualTo(bidCurrencies.toSet())
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
            val actual =
                converter.convertGetActivitiesByUser(userActivityTypes, users, blockchains, null, from, to, cursor)

            // then
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
            val actual =
                converter.convertGetActivitiesByUser(userActivityTypes, users, blockchains, null, from, to, cursor)

            // then
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(ActivityTypeDto.TRANSFER, ActivityTypeDto.SELL)
            assertThat(actual.blockchains).containsExactlyInAnyOrder(*blockchains.toTypedArray())
            assertThat(actual.anyUsers).isEmpty()
            assertThat(actual.usersTo).containsExactlyInAnyOrder("loupa", "poupa")
            assertThat(actual.from).isEqualTo(from)
            assertThat(actual.to).isEqualTo(to)
            assertThat(actual.cursor).isEqualTo(cursor)
        }

        @Test
        fun `should convert - sell and buy should convert to anyUsers`() {
            // given
            val userActivityTypes = listOf(UserActivityTypeDto.SELL, UserActivityTypeDto.BUY)
            val users = listOf("SOLANA:loupa", "FLOW:poupa")
            val blockchains = listOf(BlockchainDto.SOLANA, BlockchainDto.FLOW)
            val from = Instant.ofEpochMilli(12345)
            val to = Instant.ofEpochMilli(67890)
            val cursor = "some cursor"

            // when
            val actual =
                converter.convertGetActivitiesByUser(userActivityTypes, users, blockchains, null, from, to, cursor)

            // then
            assertThat(actual.activityTypes).containsExactlyInAnyOrder(ActivityTypeDto.SELL)
            assertThat(actual.anyUsers).containsExactlyInAnyOrder("loupa", "poupa")
        }

        @Test
        fun `should convert - null blockchains`() {
            // given
            val userActivityTypes =
                listOf(UserActivityTypeDto.TRANSFER_FROM, UserActivityTypeDto.TRANSFER_TO, UserActivityTypeDto.BUY)
            val users = listOf("SOLANA:loupa", "FLOW:poupa")
            val from = Instant.ofEpochMilli(12345)
            val to = Instant.ofEpochMilli(67890)
            val cursor = "some cursor"

            // when
            val actual = converter.convertGetActivitiesByUser(userActivityTypes, users, null, null, from, to, cursor)

            // then
            assertThat(actual).usingRecursiveComparison()
                .ignoringFields("activityTypes", "anyUsers", "from", "to", "cursor")
                .isEqualTo(emptyGenericFilter)
        }
    }
}
