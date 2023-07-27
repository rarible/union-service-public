package com.rarible.protocol.union.worker.job.meta

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.worker.config.MocaXpCustomAttributesProviderProperties
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

@ManualTest
class MocaXpMetaCustomAttributesProviderMt {

    private val provider = MocaXpMetaCustomAttributesProvider(
        properties = MocaXpCustomAttributesProviderProperties(
            enabled = true,
            baseUrl = "https://api-dev.mocaverse.xyz",
            collection = "ETHEREUM:0x759febe563dbb20e14d1c220dd10842a7d375137",
            uri = "/moca-xp/all_moca_xp.json",
            apiKey = ""
        ),
        mockk {
            every { customize(any()) } answers {
                val builder = it.invocation.args[0] as WebClient.Builder
                builder.codecs { configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024) }
            }
        }
    )

    @Test
    fun `get custom attributes`() = runBlocking<Unit> {
        val result = provider.getCustomAttributes()
        result.take(5).forEach {
            println(it)
        }
    }
}
