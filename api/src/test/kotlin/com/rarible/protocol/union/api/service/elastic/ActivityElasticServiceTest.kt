package com.rarible.protocol.union.api.service.elastic

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ActivityElasticServiceTest {

    @MockK
    private lateinit var activityFilterConverter: ActivityFilterConverter

    @InjectMockKs
    private lateinit var service: ActivityElasticService

    @Nested
    inner class GetAllActivitiesTest {

        @Test
        fun `should get all activities`() {
            // given

            // when
            // TODO uncomment when implemented
            //service.getAllActivities(type, blockchains, continuation, cursor, size, sort)

            // then
        }
    }

    @Nested
    inner class GetActivitiesByCollectionTest {

        @Test
        fun `should get activities by collection`() {
            // given

            // when
            // TODO uncomment when implemented
            //service.getActivitiesByCollection(type, collection, continuation, cursor, size, sort)

            // then
        }
    }

    @Nested
    inner class GetActivitiesByItemTest {

        @Test
        fun `should get activities by item`() {
            // given

            // when
            // TODO uncomment when implemented
            //service.getActivitiesByItem(type, itemId, continuation, cursor, size, sort)

            // then
        }
    }

    @Nested
    inner class GetActivitiesByUserTest {

        @Test
        fun `should get activities by user`() {
            // given

            // when
            // TODO uncomment when implemented
            //service.getActivitiesByUser(type, user, blockchains, from, to, continuation, cursor, size, sort)

            // then
        }
    }
}
