package com.rarible.protocol.union.worker.job.meta

import com.rarible.protocol.union.core.test.ManualTest
import com.rarible.protocol.union.worker.config.MocaXpCustomAttributesProviderProperties
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@ManualTest
class MocaXpMetaCustomAttributesProviderMt {

    private val provider = MocaXpMetaCustomAttributesProvider(
        properties = MocaXpCustomAttributesProviderProperties(
            enabled = true,
            baseUrl = "https://api-dev.mocaverse.xyz",
            collection = "ETHEREUM:0x759febe563dbb20e14d1c220dd10842a7d375137",
            uri = "/moca-xp/all_moca_xp.json",
            apiKey = ""
        )
    )

    @Test
    fun `get custom attributes`() = runBlocking<Unit> {
        val result = provider.getCustomAttributes()
        result.take(5).forEach {
            println(it)
        }
    }

}