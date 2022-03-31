package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import org.springframework.stereotype.Service

@Service
class UserActivityTypeConverter {

    fun convert(userActivityType: UserActivityTypeDto): ActivityAndUserType {
        return when (userActivityType) {
            UserActivityTypeDto.TRANSFER_FROM ->
                ActivityAndUserType(
                    ActivityTypeDto.TRANSFER,
                    isMaker = true,
                )
            UserActivityTypeDto.TRANSFER_TO ->
                ActivityAndUserType(
                    ActivityTypeDto.TRANSFER,
                    isMaker = false,
                )
            UserActivityTypeDto.MINT ->
                ActivityAndUserType(
                    ActivityTypeDto.MINT,
                    isMaker = true,
                )
            UserActivityTypeDto.BURN ->
                ActivityAndUserType(
                    ActivityTypeDto.BURN,
                    isMaker = true,
                )
            UserActivityTypeDto.MAKE_BID ->
                ActivityAndUserType(
                    ActivityTypeDto.BID,
                    isMaker = true,
                )
            UserActivityTypeDto.GET_BID ->
                ActivityAndUserType(
                    ActivityTypeDto.BID,
                    isMaker = false,
                )
            UserActivityTypeDto.LIST ->
                ActivityAndUserType(
                    ActivityTypeDto.LIST,
                    isMaker = true,
                )
            UserActivityTypeDto.BUY ->
                ActivityAndUserType(
                    ActivityTypeDto.SELL,
                    isMaker = false,
                )
            UserActivityTypeDto.SELL ->
                ActivityAndUserType(
                    ActivityTypeDto.SELL,
                    isMaker = true,
                )
            UserActivityTypeDto.CANCEL_LIST ->
                ActivityAndUserType(
                    ActivityTypeDto.CANCEL_LIST,
                    isMaker = true,
                )
            UserActivityTypeDto.CANCEL_BID ->
                ActivityAndUserType(
                    ActivityTypeDto.CANCEL_BID,
                    isMaker = true,
                )
            UserActivityTypeDto.AUCTION_BID ->
                ActivityAndUserType(
                    ActivityTypeDto.AUCTION_BID,
                    isMaker = true,
                )
            UserActivityTypeDto.AUCTION_CREATED ->
                ActivityAndUserType(
                    ActivityTypeDto.AUCTION_CREATED,
                    isMaker = true,
                )
            UserActivityTypeDto.AUCTION_CANCEL ->
                ActivityAndUserType(
                    ActivityTypeDto.AUCTION_CANCEL,
                    isMaker = true,
                )
            UserActivityTypeDto.AUCTION_FINISHED ->
                ActivityAndUserType(
                    ActivityTypeDto.AUCTION_FINISHED,
                    isMaker = true,
                )
            UserActivityTypeDto.AUCTION_STARTED ->
                ActivityAndUserType(
                    ActivityTypeDto.AUCTION_STARTED,
                    isMaker = true,
                )
            UserActivityTypeDto.AUCTION_ENDED ->
                ActivityAndUserType(
                    ActivityTypeDto.AUCTION_ENDED,
                    isMaker = true,
                )
        }
    }

    data class ActivityAndUserType(
        val activityTypeDto: ActivityTypeDto,
        val isMaker: Boolean,
    )
}
