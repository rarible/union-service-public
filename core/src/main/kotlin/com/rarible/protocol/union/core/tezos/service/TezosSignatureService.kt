package com.rarible.protocol.union.core.tezos.service

import com.rarible.protocol.tezos.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SignatureValidationFormDto
import kotlinx.coroutines.reactive.awaitFirst

class TezosSignatureService(
    blockchain: BlockchainDto,
    private val signatureControllerApi: OrderSignatureControllerApi
) : AbstractBlockchainService(blockchain), SignatureService {

    override suspend fun validate(form: SignatureValidationFormDto): Boolean {
        val tezosForm = com.rarible.protocol.tezos.dto.SignatureValidationFormDto(
            signer = form.signer.value,
            message = form.message,
            signature = form.signature
        )
        return signatureControllerApi.validate(tezosForm).awaitFirst()
    }
}
