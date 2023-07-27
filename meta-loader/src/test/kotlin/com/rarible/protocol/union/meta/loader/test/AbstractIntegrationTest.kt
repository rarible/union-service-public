package com.rarible.protocol.union.meta.loader.test

import com.rarible.core.content.meta.loader.ContentMetaReceiver
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@Suppress("UNCHECKED_CAST")
abstract class AbstractIntegrationTest {

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @Autowired
    @Qualifier("test.content.meta.receiver")
    lateinit var testContentMetaReceiver: ContentMetaReceiver
}
