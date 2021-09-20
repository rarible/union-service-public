package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.SignatureServiceRouter
import com.rarible.protocol.union.dto.IdParser
import com.rarible.protocol.union.dto.UnionSignatureValidationFormDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SignatureController(
    private val router: SignatureServiceRouter
) : SignatureControllerApi {

    override suspend fun validate(form: UnionSignatureValidationFormDto): ResponseEntity<Boolean> {
        val (blockchain, shortSigner) = IdParser.parse(form.signer)
        val blockchainForm = form.copy(signer = shortSigner)
        val result = router.getService(blockchain).validate(blockchainForm)
        return ResponseEntity.ok(result)
    }
}