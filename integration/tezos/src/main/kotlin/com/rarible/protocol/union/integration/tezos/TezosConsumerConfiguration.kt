package com.rarible.protocol.union.integration.tezos

import com.rarible.protocol.union.integration.tezos.dipdup.DipDupConsumerConfiguration
import org.springframework.context.annotation.Import

@TezosConfiguration
@Import(value = [TezosApiConfiguration::class, DipDupConsumerConfiguration::class])
class TezosConsumerConfiguration
