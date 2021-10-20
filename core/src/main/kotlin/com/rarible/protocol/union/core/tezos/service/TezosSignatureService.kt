package com.rarible.protocol.union.core.tezos.service

import com.rarible.protocol.tezos.api.client.OrderSignatureControllerApi
import com.rarible.protocol.tezos.dto.SignatureValidationFormDto
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.reactive.awaitFirst

class TezosSignatureService(
    blockchain: BlockchainDto,
    private val signatureControllerApi: OrderSignatureControllerApi
) : AbstractBlockchainService(blockchain), SignatureService {

    override suspend fun validate(
        signer: String,
        signature: String,
        message: String
    ): Boolean {
        val tezosForm = SignatureValidationFormDto(
            signer = signer,
            signature = signature,
            message = message
        )
        return signatureControllerApi.validate(tezosForm).awaitFirst()
    }
}
