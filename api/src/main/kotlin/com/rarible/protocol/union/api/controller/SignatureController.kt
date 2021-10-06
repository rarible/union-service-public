package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.SignatureValidationFormDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SignatureController(
    private val router: BlockchainRouter<SignatureService>
) : SignatureControllerApi {

    override suspend fun validate(form: SignatureValidationFormDto): ResponseEntity<Boolean> {
        val result = router.getService(form.signer.blockchain).validate(form)
        return ResponseEntity.ok(result)
    }
}
