package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.SignatureValidationFormDto

interface SignatureService : BlockchainService {

    suspend fun validate(form: SignatureValidationFormDto): Boolean

}