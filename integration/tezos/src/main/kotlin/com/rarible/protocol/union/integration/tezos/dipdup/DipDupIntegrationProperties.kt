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
    val tzktUrl: String,
    val tzktProperties: TzktProperties = TzktProperties(),
    val ipfsUrl: String,
    val nodeAddress: String,
    val chainId: String,
    val sigChecker: String,
    val knownAddresses: KnownAddresses?,
    val consumer: DefaultConsumerProperties?,
    val network: String,
    val fungibleContracts: Set<String> = emptySet(),
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties()
) {

    data class TzktProperties(
        val nftChecking: Boolean = true,
        val retryAttempts: Int = 5,
        val retryDelay: Long = 15_000, // ms
        val ignorePeriod: Long = 1000 * 3600 * 24 // 1 day period
    )

}
