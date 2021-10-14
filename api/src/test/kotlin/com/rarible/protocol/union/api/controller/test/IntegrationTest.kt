package com.rarible.protocol.union.api.controller.test

import com.rarible.core.test.ext.MongoCleanup
import com.rarible.core.test.ext.MongoTest
import com.rarible.core.test.ext.RedisTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@MongoTest
@MongoCleanup
@RedisTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["spring.cloud.bootstrap.enabled=false"]
)
@ActiveProfiles("test")
@Import(value = [TestApiConfiguration::class])
annotation class IntegrationTest