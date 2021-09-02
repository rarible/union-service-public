package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.union.dto.FlowBlockchainDto

object FlowBlockchainConverter {

    // TODO there is no Blockchain enum in Flow for now
    fun convert(): FlowBlockchainDto {
        return FlowBlockchainDto.FLOW
    }
}