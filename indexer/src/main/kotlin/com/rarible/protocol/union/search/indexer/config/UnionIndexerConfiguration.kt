package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.integration.ethereum.blockchain.ethereum.EthereumApiConfiguration
import com.rarible.protocol.union.integration.ethereum.blockchain.mantle.MantleApiConfiguration
import com.rarible.protocol.union.integration.ethereum.blockchain.polygon.PolygonApiConfiguration
import com.rarible.protocol.union.integration.flow.FlowApiConfiguration
import com.rarible.protocol.union.integration.immutablex.ImxApiConfiguration
import com.rarible.protocol.union.integration.solana.SolanaApiConfiguration
import com.rarible.protocol.union.integration.tezos.TezosApiConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableRaribleMongo
@EnableConfigurationProperties(value = [KafkaProperties::class, IndexerProperties::class])
@ComponentScan(basePackageClasses = [EsActivityRepository::class])
@Import(
    value = [
        EthereumApiConfiguration::class,
        PolygonApiConfiguration::class,
        FlowApiConfiguration::class,
        TezosApiConfiguration::class, // TODO enable with proper config
        SolanaApiConfiguration::class,
        ImxApiConfiguration::class,
        MantleApiConfiguration::class,
        SearchConfiguration::class,
        CoreConfiguration::class,
    ]
)
class UnionIndexerConfiguration
