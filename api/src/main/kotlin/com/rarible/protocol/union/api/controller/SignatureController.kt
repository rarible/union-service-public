package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.SignatureServiceRouter
import com.rarible.protocol.union.dto.SignatureValidationFormDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SignatureController(
    private val router: SignatureServiceRouter
) : SignatureControllerApi {

    override suspend fun validate(form: SignatureValidationFormDto): ResponseEntity<Boolean> {
        val result = router.getService(form.signer.blockchain).validate(form)
        return ResponseEntity.ok(result)
    }
}
