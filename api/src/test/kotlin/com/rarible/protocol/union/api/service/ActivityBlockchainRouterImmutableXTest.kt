package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource


@IntegrationTest
@TestPropertySource(properties = ["common.feature-flags.enableImmutableXActivitiesQueries=true"])
class ActivityBlockchainRouterWithImmutableXEnabledTest {

    @Autowired
    private lateinit var router: BlockchainRouter<ActivityService>

    @Test
    fun `immutableX is enabled`() {
        // when
        val actual = router.getEnabledBlockchains(emptyList())

        // then
        assertThat(actual).contains(BlockchainDto.IMMUTABLEX)
    }
}

@IntegrationTest
@TestPropertySource(properties = ["common.feature-flags.enableImmutableXActivitiesQueries=false"])
class ActivityBlockchainRouterWithImmutableXDisabledTest {

    @Autowired
    private lateinit var router: BlockchainRouter<ActivityService>

    @Test
    fun `immutableX is disabled`() {
        // when
        val actual = router.getEnabledBlockchains(emptyList())

        // then
        assertThat(actual).doesNotContain(BlockchainDto.IMMUTABLEX)
    }
}
