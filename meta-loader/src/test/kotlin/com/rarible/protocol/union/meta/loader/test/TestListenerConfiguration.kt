package com.rarible.protocol.union.meta.loader.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.protocol.union.core.CoreConfiguration
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@TestConfiguration
@Import(CoreConfiguration::class)
class TestListenerConfiguration {

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    @Bean
    @Primary
    @Qualifier("test.content.meta.receiver")
    fun testContentMetaReceiver(): ContentMetaReceiver = mockk()
}
