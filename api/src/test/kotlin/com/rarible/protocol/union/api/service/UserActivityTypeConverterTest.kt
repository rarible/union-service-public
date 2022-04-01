package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream


class UserActivityTypeConverterTest {

    private val converter = UserActivityTypeConverter()

    companion object {
        @JvmStatic
        fun userActivityTypes() = Stream.of(
            Arguments.of(
                UserActivityTypeDto.TRANSFER_FROM,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.TRANSFER,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.TRANSFER_TO,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.TRANSFER,
                    isMaker = false
                )
            ),
            Arguments.of(
                UserActivityTypeDto.MINT,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.MINT,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.BURN,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.BURN,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.MAKE_BID,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.BID,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.GET_BID,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.BID,
                    isMaker = false
                )
            ),
            Arguments.of(
                UserActivityTypeDto.LIST,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.LIST,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.BUY,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.SELL,
                    isMaker = false
                )
            ),
            Arguments.of(
                UserActivityTypeDto.SELL,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.SELL,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.CANCEL_LIST,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.CANCEL_LIST,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.CANCEL_BID,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.CANCEL_BID,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.AUCTION_BID,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.AUCTION_BID,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.AUCTION_CREATED,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.AUCTION_CREATED,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.AUCTION_CANCEL,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.AUCTION_CANCEL,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.AUCTION_FINISHED,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.AUCTION_FINISHED,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.AUCTION_STARTED,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.AUCTION_STARTED,
                    isMaker = true
                )
            ),
            Arguments.of(
                UserActivityTypeDto.AUCTION_ENDED,
                UserActivityTypeConverter.ActivityAndUserType(
                    ActivityTypeDto.AUCTION_ENDED,
                    isMaker = true
                )
            ),
        )
    }

    @ParameterizedTest
    @MethodSource("userActivityTypes")
    fun `should convert`(
        userActivityType: UserActivityTypeDto,
        expected: UserActivityTypeConverter.ActivityAndUserType
    ) {
        // when
        val actual = converter.convert(userActivityType)

        // then
        assertThat(actual).isEqualTo(expected)
    }
}
