package com.rarible.protocol.union.core

import com.rarible.protocol.union.dto.PlatformDto

object Platform {

    fun isRarible(platformDto: PlatformDto?): Boolean {
        return platformDto == null || platformDto == PlatformDto.ALL || platformDto == PlatformDto.RARIBLE
    }

}