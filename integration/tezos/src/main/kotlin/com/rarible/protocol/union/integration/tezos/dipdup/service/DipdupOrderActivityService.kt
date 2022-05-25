package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.model.DipDupActivityType
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.continuation.page.Slice

interface DipdupOrderActivityService {

    fun enabled() = false

    suspend fun getAll(types: List<ActivityTypeDto>, continuation: String?, limit: Int, sort: ActivitySortDto?): Slice<ActivityDto> {
        TODO("Not implemented")
    }

    suspend fun getByIds(ids: List<String>): List<ActivityDto> {
        TODO("Not implemented")
    }

}
