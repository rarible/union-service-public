package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.EthMetaStatusDto
import com.rarible.protocol.union.core.exception.UnionMetaException

object MetaStatusChecker {

    fun checkStatus(status: EthMetaStatusDto, entityId: String) {
        when (status) {
            EthMetaStatusDto.UNPARSEABLE_LINK -> throw UnionMetaException(
                UnionMetaException.ErrorCode.CORRUPTED_URL,
                "Can't parse meta url for $entityId"
            )

            EthMetaStatusDto.UNPARSEABLE_JSON -> throw UnionMetaException(
                UnionMetaException.ErrorCode.CORRUPTED_DATA,
                "Can't parse meta json for $entityId"
            )

            EthMetaStatusDto.TIMEOUT -> throw UnionMetaException(
                UnionMetaException.ErrorCode.TIMEOUT,
                "Timeout during get meta for $entityId"
            )

            EthMetaStatusDto.ERROR -> throw UnionMetaException(
                UnionMetaException.ErrorCode.ERROR,
                "Unexpected exception during get meta for $entityId"
            )

            else -> {}
        }
    }

}