package com.rarible.protocol.union.enrichment.configuration

import com.rarible.protocol.union.integration.ethereum.EthereumApiConfiguration
import com.rarible.protocol.union.integration.ethereum.PolygonApiConfiguration
import com.rarible.protocol.union.integration.flow.FlowApiConfiguration
import com.rarible.protocol.union.integration.tezos.TezosApiConfiguration
import com.rarible.protocol.union.integration.solana.SolanaApiConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    value = [
        EnrichmentConfiguration::class,

        EthereumApiConfiguration::class,
        PolygonApiConfiguration::class,
        FlowApiConfiguration::class,
        TezosApiConfiguration::class,
        SolanaApiConfiguration::class
    ]
)
class EnrichmentApiConfiguration
