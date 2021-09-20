package com.rarible.protocol.union.core.service

import org.springframework.stereotype.Component

@Component
class SignatureServiceRouter(
    signatureServices: List<SignatureService>
) : BlockchainRouter<SignatureService>(
    signatureServices
)