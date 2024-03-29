package com.rarible.protocol.union.listener.repository

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.model.EnrichmentActivityId
import com.rarible.protocol.union.enrichment.model.EnrichmentBurnActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentMintActivity
import com.rarible.protocol.union.enrichment.model.EnrichmentOrderMatchSell
import com.rarible.protocol.union.enrichment.model.EnrichmentTransferActivity
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.temporal.ChronoUnit

@IntegrationTest
class ActivityRepositoryIt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var activityRepository: ActivityRepository

    @Test
    fun crud() = runBlocking<Unit> {
        val activity1 =
            activityRepository.save(
                EnrichmentMintActivity(
                    activityId = "1",
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                    collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                    itemId = itemIdDto().fullId(),
                    tokenId = BigInteger.ONE,
                    date = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    owner = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.TWO().toString()
                    ),
                    transactionHash = randomString(),
                    value = BigInteger.ONE,
                )
            )
        val activity2 =
            activityRepository.save(
                EnrichmentBurnActivity(
                    activityId = "2",
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                    collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                    itemId = itemIdDto().fullId(),
                    tokenId = BigInteger.ONE,
                    date = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    owner = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.TWO().toString()
                    ),
                    transactionHash = randomString(),
                    value = BigInteger.ONE,
                )
            )

        val activityId1 = EnrichmentActivityId(blockchain = BlockchainDto.ETHEREUM, activityId = "1")
        val activityId2 = EnrichmentActivityId(blockchain = BlockchainDto.ETHEREUM, activityId = "2")
        assertThat(
            activityRepository.getAll(
                listOf(
                    activityId2,
                    EnrichmentActivityId(blockchain = BlockchainDto.ETHEREUM, activityId = "3"),
                    activityId1
                )
            )
        ).containsExactly(activity2, activity1)

        val found = activityRepository.get(activityId1) as EnrichmentMintActivity
        assertThat(found).isEqualTo(activity1)

        activityRepository.save(found.copy(mintPrice = BigDecimal.TEN))

        val found2 = activityRepository.get(activityId1) as EnrichmentMintActivity
        assertThat(found2).isEqualTo(activity1.copy(mintPrice = BigDecimal.TEN))

        activityRepository.delete(activityId1)

        assertThat(activityRepository.get(activityId1)).isNull()

        val found3 = activityRepository.get(activity2.id)
        assertThat(found3).isEqualTo(activity2)
    }

    @Test
    fun findLastSale() = runBlocking<Unit> {
        val activity1 =
            activityRepository.save(
                EnrichmentMintActivity(
                    activityId = "1",
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                    collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                    itemId = itemIdDto().fullId(),
                    tokenId = BigInteger.ONE,
                    date = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    owner = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.TWO().toString()
                    ),
                    transactionHash = randomString(),
                    value = BigInteger.ONE,
                )
            )
        val activity2 =
            activityRepository.save(
                EnrichmentOrderMatchSell(
                    activityId = "2",
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                    collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                    itemId = itemIdDto().fullId(),
                    date = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.SECONDS),
                    transactionHash = randomString(),
                    buyer = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.TWO().toString()
                    ),
                    nft = UnionAsset(
                        type = UnionEthErc721AssetType(
                            contract = ContractAddress(
                                blockchain = BlockchainDto.ETHEREUM,
                                value = Address.ONE().toString()
                            ),
                            tokenId = BigInteger.ONE,
                        ),
                        value = BigDecimal.ONE,
                    ),
                    payment = UnionAsset(
                        type = UnionEthEthereumAssetType(
                            blockchain = BlockchainDto.ETHEREUM
                        ),
                        value = BigDecimal.ONE,
                    ),
                    price = BigDecimal.ONE,
                    seller = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.THREE().toString()
                    ),
                    source = OrderActivitySourceDto.RARIBLE,
                    type = EnrichmentOrderMatchSell.Type.SELL
                )
            )
        val activity3 =
            activityRepository.save(
                EnrichmentOrderMatchSell(
                    activityId = "3",
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                    collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                    itemId = itemIdDto().fullId(),
                    date = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    transactionHash = randomString(),
                    buyer = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.TWO().toString()
                    ),
                    nft = UnionAsset(
                        type = UnionEthErc721AssetType(
                            contract = ContractAddress(
                                blockchain = BlockchainDto.ETHEREUM,
                                value = Address.ONE().toString()
                            ),
                            tokenId = BigInteger.ONE,
                        ),
                        value = BigDecimal.ONE,
                    ),
                    payment = UnionAsset(
                        type = UnionEthEthereumAssetType(
                            blockchain = BlockchainDto.ETHEREUM
                        ),
                        value = BigDecimal.ONE,
                    ),
                    price = BigDecimal.ONE,
                    seller = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.THREE().toString()
                    ),
                    source = OrderActivitySourceDto.RARIBLE,
                    type = EnrichmentOrderMatchSell.Type.SELL
                )
            )
        val activity4 =
            activityRepository.save(
                EnrichmentOrderMatchSell(
                    activityId = "4",
                    blockchain = BlockchainDto.ETHEREUM,
                    contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                    collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                    itemId = ItemIdDto(
                        blockchain = BlockchainDto.ETHEREUM,
                        contract = Address.ONE().toString(),
                        tokenId = BigInteger("2")
                    ).fullId(),
                    date = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                    transactionHash = randomString(),
                    buyer = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.TWO().toString()
                    ),
                    nft = UnionAsset(
                        type = UnionEthErc721AssetType(
                            contract = ContractAddress(
                                blockchain = BlockchainDto.ETHEREUM,
                                value = Address.ONE().toString()
                            ),
                            tokenId = BigInteger("2"),
                        ),
                        value = BigDecimal.ONE,
                    ),
                    payment = UnionAsset(
                        type = UnionEthEthereumAssetType(
                            blockchain = BlockchainDto.ETHEREUM
                        ),
                        value = BigDecimal.ONE,
                    ),
                    price = BigDecimal.ONE,
                    seller = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.ETHEREUM,
                        value = Address.THREE().toString()
                    ),
                    source = OrderActivitySourceDto.RARIBLE,
                    type = EnrichmentOrderMatchSell.Type.SELL
                )
            )

        assertThat(activityRepository.findLastSale(itemIdDto())).isEqualTo(activity3)

        activityRepository.delete(activity3.id)

        assertThat(activityRepository.findLastSale(itemIdDto())).isEqualTo(activity2)

        activityRepository.delete(activity2.id)

        assertThat(activityRepository.findLastSale(itemIdDto())).isNull()
    }

    @Test
    fun isMinter() = runBlocking<Unit> {
        val itemId = itemIdDto()
        val owner = UnionAddress(
            blockchainGroup = BlockchainGroupDto.ETHEREUM,
            value = Address.TWO().toString()
        )
        activityRepository.save(
            EnrichmentMintActivity(
                activityId = "1",
                blockchain = BlockchainDto.ETHEREUM,
                contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                itemId = itemId.fullId(),
                tokenId = BigInteger.ONE,
                date = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                owner = owner,
                transactionHash = randomString(),
                value = BigInteger.ONE,
            )
        )

        assertThat(activityRepository.isMinter(itemId, owner)).isTrue()
        assertThat(
            activityRepository.isMinter(
                itemId, UnionAddress(
                    blockchainGroup = BlockchainGroupDto.ETHEREUM,
                    value = Address.FOUR().toString()
                )
            )
        ).isFalse()
    }

    @Test
    fun isBuyer() = runBlocking<Unit> {
        val itemId = itemIdDto()
        val owner = UnionAddress(
            blockchainGroup = BlockchainGroupDto.ETHEREUM,
            value = Address.TWO().toString()
        )
        val owner2 = UnionAddress(blockchainGroup = BlockchainGroupDto.ETHEREUM, value = Address.THREE().toString())
        activityRepository.save(
            EnrichmentTransferActivity(
                activityId = "1",
                blockchain = BlockchainDto.ETHEREUM,
                contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                itemId = itemId.fullId(),
                tokenId = BigInteger.ONE,
                date = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                owner = owner,
                transactionHash = randomString(),
                value = BigInteger.ONE,
                purchase = true,
                from = owner2
            )
        )
        activityRepository.save(
            EnrichmentTransferActivity(
                activityId = "2",
                blockchain = BlockchainDto.ETHEREUM,
                contract = ContractAddress(blockchain = BlockchainDto.ETHEREUM, value = Address.ONE().toString()),
                collection = CollectionIdDto(blockchain = BlockchainDto.ETHEREUM, value = "2").fullId(),
                itemId = itemId.fullId(),
                tokenId = BigInteger.ONE,
                date = Instant.now().truncatedTo(ChronoUnit.SECONDS),
                owner = owner2,
                transactionHash = randomString(),
                value = BigInteger.ONE,
                purchase = false,
                from = owner
            )
        )

        assertThat(activityRepository.isBuyer(itemId, owner)).isTrue()
        assertThat(
            activityRepository.isBuyer(
                itemId, UnionAddress(
                    blockchainGroup = BlockchainGroupDto.ETHEREUM,
                    value = Address.FOUR().toString()
                )
            )
        ).isFalse()
        assertThat(activityRepository.isBuyer(itemId, owner2)).isFalse()
    }

    private fun itemIdDto() = ItemIdDto(
        blockchain = BlockchainDto.ETHEREUM,
        contract = Address.ONE().toString(),
        tokenId = BigInteger.ONE
    )
}
