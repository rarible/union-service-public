package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.api.util.BlockchainFilter
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.SignatureValidationFormDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SignatureController(
    private val router: BlockchainRouter<SignatureService>,
    private val blockchainFilter: BlockchainFilter
) : SignatureControllerApi {

    override suspend fun validate(form: SignatureValidationFormDto): ResponseEntity<Boolean> {
        val result = router.getService(blockchainFilter.getEnabledInApi(form.signer.blockchainGroup)[0])
            .validate(form.signer.value, form.publicKey, form.signature, form.message)
        return ResponseEntity.ok(result)
    }
}
