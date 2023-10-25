package com.rarible.protocol.union.api.configuration

import com.rarible.core.daemon.DaemonWorkerProperties
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration
import java.time.Instant

@ConstructorBinding
@ConfigurationProperties("api")
data class ApiProperties(
    val domains: Map<BlockchainDto, List<String>> = emptyMap(),
    val openapi: OpenapiProperties = OpenapiProperties(),
    val subscribe: SubscribeProperties = SubscribeProperties(),
    val elasticsearch: EsProperties = EsProperties(),
    val orderSettings: OrderSettingsProperties = OrderSettingsProperties()
) {

    fun findBlockchain(topDomain: String): BlockchainDto? {
        return domains.entries.find { (_, domains) -> topDomain in domains }?.key
    }
}

data class OpenapiProperties(
    val baseUrl: String = "",
    val description: String = "",
    // Map of examples for openapi, like "itemId" -> "ETHEREUM:0x123"
    // In openapi examples should be declared as ${itemId}
    val examples: Map<String, String> = mapOf()
)

data class SubscribeProperties(
    val daemon: DaemonWorkerProperties = DaemonWorkerProperties(),
    val workers: Map<String, Int> = mapOf()
)

data class EsProperties(
    val itemsTraitsKeysLimit: Int = 200,
    val itemsTraitsValuesLimit: Int = 200,
    val optimization: EsOptimizationProperties = EsOptimizationProperties()
)

data class EsOptimizationProperties(
    val lastUpdatedSearchPeriod: Duration = Duration.ofMinutes(30),
    val earliestItemByLastUpdateAt: Instant = Instant.parse("2016-04-26T20:30:48.000Z"),
    val earliestActivityByDate: Instant = Instant.parse("2016-04-26T06:08:46.000Z"),
    val activityDateSearchPeriod: Duration = Duration.ofDays(7),
    val earliestOwnershipDate: Instant = Instant.parse("2016-04-26T20:30:48.000Z"),
    val ownershipDateSearchPeriod: Duration = Duration.ofDays(7),
)

data class OrderSettingsProperties(
    val fees: Map<BlockchainDto, Map<String, Int>> = emptyMap()
)
