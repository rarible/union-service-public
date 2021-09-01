package com.rarible.protocol.union.listener.test

import com.rarible.core.test.ext.KafkaTest
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@KafkaTest
@EnableAutoConfiguration
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = ["spring.cloud.bootstrap.enabled=false"]
)
@Import(value = [IntegrationTestConfiguration::class])
@ActiveProfiles("test")
annotation class IntegrationTest