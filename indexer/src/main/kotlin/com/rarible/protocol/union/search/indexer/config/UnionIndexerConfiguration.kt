package com.rarible.protocol.union.search.indexer.config

import com.rarible.protocol.union.core.CoreConfiguration
import com.rarible.protocol.union.enrichment.configuration.EnrichmentApiConfiguration
import com.rarible.protocol.union.enrichment.configuration.EnrichmentConfiguration
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.integration.ethereum.EthereumApiConfiguration
import com.rarible.protocol.union.integration.ethereum.PolygonApiConfiguration
import com.rarible.protocol.union.integration.flow.FlowApiConfiguration
import com.rarible.protocol.union.integration.immutablex.ImmutablexApiConfiguration
import com.rarible.protocol.union.integration.solana.SolanaApiConfiguration
import com.rarible.protocol.union.integration.tezos.TezosApiConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@EnableConfigurationProperties(value = [KafkaProperties::class, IndexerProperties::class])
@ComponentScan(basePackageClasses = [EsActivityRepository::class])
@Import(
    value = [
        EthereumApiConfiguration::class,
        PolygonApiConfiguration::class,
        FlowApiConfiguration::class,
        TezosApiConfiguration::class,
        SolanaApiConfiguration::class,
        ImmutablexApiConfiguration::class,
        SearchConfiguration::class,
        CoreConfiguration::class,
    ]
)
class UnionIndexerConfiguration