package com.rarible.protocol.union.enrichment.configuration

import com.rarible.protocol.union.integration.ethereum.EthConsumerConfiguration
import com.rarible.protocol.union.integration.flow.FlowConsumerConfiguration
import com.rarible.protocol.union.integration.immutablex.ImxConsumerConfiguration
import com.rarible.protocol.union.integration.solana.SolanaConsumerConfiguration
import com.rarible.protocol.union.integration.tezos.TezosConsumerConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    value = [
        EnrichmentApiConfiguration::class,
        EthConsumerConfiguration::class,
        FlowConsumerConfiguration::class,
        TezosConsumerConfiguration::class,
        ImxConsumerConfiguration::class,
        SolanaConsumerConfiguration::class,
        SimplehashConsumerConfiguration::class
    ]
)
class EnrichmentConsumerConfiguration
