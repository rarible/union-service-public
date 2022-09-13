package com.rarible.protocol.union.integration.tezos

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.core.ConsumerFactory
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupConsumerConfiguration
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Import

@TezosConfiguration
@Import(value = [TezosApiConfiguration::class, DipDupConsumerConfiguration::class])
class TezosConsumerConfiguration(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
    properties: TezosIntegrationProperties,
    @Value("\${rarible.core.client.k8s:false}")
    private val k8s: Boolean,
    @Value("\${integration.tezos.dipdup.enabled:false}")
    private val isDipDupEnabled: Boolean,
    private val consumerFactory: ConsumerFactory
) {

    private val env = applicationEnvironmentInfo.name
    private val host = applicationEnvironmentInfo.host

    private val consumer = properties.consumer!!
    private val workers = properties.consumer!!.workers

    private val daemon = properties.daemon

    //-------------------- Handlers -------------------//

}
