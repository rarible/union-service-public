package com.rarible.protocol.union.meta.loader.test

import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.protocol.union.enrichment.meta.UnionMetaLoader
import io.mockk.clearMocks
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@Suppress("UNCHECKED_CAST")
abstract class AbstractIntegrationTest {

    @Autowired
    @Qualifier("test.union.meta.loader")
    lateinit var testUnionMetaLoader: UnionMetaLoader

    @Autowired
    @Qualifier("test.content.meta.receiver")
    lateinit var testContentMetaReceiver: ContentMetaReceiver

    @BeforeEach
    fun cleanupMetaMocks() {
        clearMocks(testUnionMetaLoader)
    }
}
