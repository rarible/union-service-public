package com.rarible.protocol.union.integration.immutablex.service

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexApiClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexWebClientFactory
import com.rarible.protocol.union.integration.immutablex.converter.ImmutablexItemConverter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

// For manual debugging purposes
@ManualTest
class ImmutablexItemServiceIt {

    private val webClient = ImmutablexWebClientFactory.createClient(
        "https://api.ropsten.x.immutable.com/v1",
        null
    )

    private val client = ImmutablexApiClient(webClient)

    private val service = ImmutablexItemService(
        client, ImmutablexItemConverter(client)
    )

    @Test
    fun getRoyalties() = runBlocking<Unit> {
        val result = service.getItemRoyaltiesById("0x6b11e2eeabfa12ae875ddd9024665b7e7edeac68:41")
        println(result)
    }

    @Test
    fun getMeta() = runBlocking<Unit> {
        val result = service.getItemMetaById("0x1ea92417b0393eba0edddea4fb35eb4e12b3165d:518103")
        println(result)
    }
}