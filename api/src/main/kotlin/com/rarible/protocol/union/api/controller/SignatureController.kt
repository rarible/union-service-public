package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.SignatureValidationFormDto
import com.rarible.protocol.union.dto.subchains
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class SignatureController(
    private val router: BlockchainRouter<SignatureService>
) : SignatureControllerApi {

    override suspend fun validate(signatureValidationFormDto: SignatureValidationFormDto): ResponseEntity<Boolean> {
        val result = router.getService(signatureValidationFormDto.signer.blockchainGroup.subchains()[0])
            .validate(
                signatureValidationFormDto.signer.value,
                signatureValidationFormDto.publicKey,
                signatureValidationFormDto.signature,
                signatureValidationFormDto.message
            )
        return ResponseEntity.ok(result)
    }
}
