package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.SignatureInputDto
import com.rarible.protocol.union.dto.SignatureInputFormDto
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
                signer = signatureValidationFormDto.signer.value,
                publicKey = signatureValidationFormDto.publicKey,
                signature = signatureValidationFormDto.signature,
                message = signatureValidationFormDto.message,
                algorithm = signatureValidationFormDto.algorithm
            )
        return ResponseEntity.ok(result)
    }

    override suspend fun getInput(signatureInputFormDto: SignatureInputFormDto): ResponseEntity<SignatureInputDto> {
        val input = router.getService(signatureInputFormDto.blockchain)
            .getInput(signatureInputFormDto)

        return ResponseEntity.ok(SignatureInputDto(input))
    }
}
