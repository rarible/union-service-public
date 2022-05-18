package com.rarible.protocol.union.integration.tezos.dipdup

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.union.core.DefaultConsumerProperties
import com.rarible.tzkt.config.KnownAddresses
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties(prefix = "integration.tezos.dipdup")
data class DipDupIntegrationProperties(
    val dipdupUrl: String,
    val tzktUrl: String, // could be in config
    val tzktProperties: TzktProperties = TzktProperties(),
    val ipfsUrl: String,
    val knownAddresses: KnownAddresses?,
    val consumer: DefaultConsumerProperties?,
    val network: String,
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties()
) {

    data class TzktProperties(
        val retryAttempts: Int = 5,
        val retryDelay: Long = 15_000 // ms
    )

}
