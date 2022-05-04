package com.rarible.protocol.union.integration.tezos.dipdup

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.tzkt.royalties.RoyaltiesConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.tezos.dipdup")
data class DipDupIntegrationProperties(
    val dipdupUrl: String,
    val tzktUrl: String,
    val ipfsUrl: String,
    val royaltyConfig: RoyaltiesConfig?,
    val consumer: DefaultConsumerProperties?,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties()
)
