package com.rarible.protocol.union.core.test

import org.junit.jupiter.api.condition.EnabledIfSystemProperty

@EnabledIfSystemProperty(named = "RUN_MANUAL_TESTS", matches = "true")
annotation class ManualTest
