package com.rarible.protocol.union.integration.tezos.dipdup

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.tzkt.config.KnownAddresses
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.tezos.dipdup")
data class DipDupIntegrationProperties(
    val dipdupUrl: String,
    val dipdupToken: String?,
    val tzktUrl: String,
    val tzktProperties: TzktProperties = TzktProperties(),
    val ipfsUrl: String,
    val nodeAddress: String,
    val chainId: String,
    val sigChecker: String,
    val knownAddresses: KnownAddresses?,
    val marketplaces: Marketplaces = Marketplaces(),
    val consumer: DefaultConsumerProperties?,
    val network: String,
    val fungibleContracts: Set<String> = emptySet(),
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(),

    // This enables query directly to dipdup indexer
    val useDipDupTokens: Boolean = false,
    val useDipDupRoyalty: Boolean = false,
    val saveDipDupRoyalty: Boolean = false,
    val enrichDipDupCollection: Boolean = true
) {

    data class TzktProperties(
        val nftChecking: Boolean = true,
        val retryAttempts: Int = 5,
        val retryDelay: Long = 15_000, // ms
        val ignorePeriod: Long = 1000 * 3600 * 24, // 1 day period
        val tokenBatch: Boolean = false,
        val ownershipBatch: Boolean = false,
        val collectionBatch: Boolean = false,
        val requestedCollectionMeta: Boolean = false,
        val wrapActivityHashes: Boolean = false,
        val checkTokenBalance: Boolean = false
    )

    data class Marketplaces(
        val hen: Boolean = false,
        val objkt: Boolean = false,
        val objktV2: Boolean = false,
        val versum: Boolean = false,
        val teia: Boolean = false,
        val fxhashV1: Boolean = false,
        val fxhashV2: Boolean = false
    ) {

        fun getEnabledMarketplaces(): Set<TezosPlatform> {
            val result = HashSet<TezosPlatform>()
            result.add(TezosPlatform.RARIBLE_V1)
            result.add(TezosPlatform.RARIBLE_V2)
            if (hen) result.add(TezosPlatform.HEN)
            if (objkt) result.add(TezosPlatform.OBJKT_V1)
            if (objktV2) result.add(TezosPlatform.OBJKT_V2)
            if (versum) result.add(TezosPlatform.VERSUM_V1)
            if (teia) result.add(TezosPlatform.TEIA_V1)
            if (fxhashV1) result.add(TezosPlatform.FXHASH_V1)
            if (fxhashV2) result.add(TezosPlatform.FXHASH_V2)
            return result
        }
    }
}
