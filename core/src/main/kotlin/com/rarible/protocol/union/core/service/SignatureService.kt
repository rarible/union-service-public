package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.UnionSignatureValidationFormDto

interface SignatureService : BlockchainService {

    suspend fun validate(form: UnionSignatureValidationFormDto): Boolean

}