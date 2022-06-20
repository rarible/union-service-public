package com.rarible.protocol.union.api.controller.tezos

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.tezos.service.TezosCollectionService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.stream.Stream


@Testcontainers
@FlowPreview
@IntegrationTest
class TezosLegacyCollectionServiceTest : AbstractIntegrationTest() {

    companion object {

        val blockchain = BlockchainDto.TEZOS

        private fun date(str: String): Instant =
            LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
                .toInstant(ZoneOffset.UTC)

        @Container
        val container = PostgreSQLContainer<Nothing>("postgres:13.5").apply {
            withDatabaseName("postgres")
            withUsername("duke")
            withPassword("s3crEt")
            withInitScript("tezosInit.sql")
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("integration.tezos.pg.port", container::getFirstMappedPort)
            registry.add("integration.tezos.pg.host", container::getHost)
            registry.add("integration.tezos.pg.user", container::getUsername)
            registry.add("integration.tezos.pg.password", container::getPassword)
            registry.add("integration.tezos.pg.database", container::getDatabaseName)
        }

        @JvmStatic
        fun collections() = Stream.of(
            Arguments.of(
                "KT18bKocekp5ei8LvBbMjUEEc8KRpLHsJHRj",
                UnionCollection(
                    id = CollectionIdDto(BlockchainDto.TEZOS, "KT18bKocekp5ei8LvBbMjUEEc8KRpLHsJHRj"),
                    type = CollectionDto.Type.TEZOS_MT,
                    name = "STATION 020: SKULL CANDY OFFERINGS",
                    features = emptyList()
                )
            ),
            Arguments.of(
                "KT18exmo82JomAcRGTNpUvp1wATSqT63AYmF",
                UnionCollection(
                    id = CollectionIdDto(BlockchainDto.TEZOS, "KT18exmo82JomAcRGTNpUvp1wATSqT63AYmF"),
                    type = CollectionDto.Type.TEZOS_MT,
                    name = "Spiral",
                    symbol = "S22",
                    features = listOf(CollectionDto.Features.BURN, CollectionDto.Features.SECONDARY_SALE_FEES),
                    owner = UnionAddress(BlockchainGroupDto.TEZOS, "tz1e6t4FD8zV46wsBndy5qcx1ch87hn6F98H"),
                    minters = listOf(UnionAddress(BlockchainGroupDto.TEZOS, "tz1e6t4FD8zV46wsBndy5qcx1ch87hn6F98H"))
                )
            ),
            Arguments.of(
                "KT1Gg9cdNu3cFUT2UrUZKBSvTRH8N8AsPk2i",
                UnionCollection(
                    id = CollectionIdDto(BlockchainDto.TEZOS, "KT1Gg9cdNu3cFUT2UrUZKBSvTRH8N8AsPk2i"),
                    type = CollectionDto.Type.TEZOS_MT,
                    name = "Unnamed Collection",
                    features = listOf(CollectionDto.Features.BURN, CollectionDto.Features.SECONDARY_SALE_FEES),
                    owner = UnionAddress(BlockchainGroupDto.TEZOS, "tz1RahNvmiJqbymNWJt3Ss9ooNjKRvEKEhpB"),
                    minters = listOf(
                        UnionAddress(BlockchainGroupDto.TEZOS, "tz1RahNvmiJqbymNWJt3Ss9ooNjKRvEKEhpB"),
                        UnionAddress(BlockchainGroupDto.TEZOS, "KT1NF8YjHkJPEfFqfQtcWAjnnrqtMHCbKWqb"),
                        UnionAddress(BlockchainGroupDto.TEZOS, "KT1NLutkkpwLZ7HWKpWbYPW6FZkzZAkZPXvX"),
                    )
                )
            )
        )
    }

    @Autowired
    lateinit var tezosCollectionService: TezosCollectionService

    @ParameterizedTest
    @MethodSource("collections")
    fun `should get collection by id list`(id: String, collection: UnionCollection) =
        runBlocking<Unit> {
            val collections = tezosCollectionService.getCollectionsByIds(listOf(id))

            assertThat(collections.first())
                .usingRecursiveComparison()
                .isEqualTo(collection)
        }
}
