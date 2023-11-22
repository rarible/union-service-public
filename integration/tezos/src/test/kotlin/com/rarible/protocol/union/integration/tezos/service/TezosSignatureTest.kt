package com.rarible.protocol.union.integration.tezos.service

import com.mongodb.assertions.Assertions.assertTrue
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktSignatureServiceImpl
import com.rarible.tzkt.client.SignatureClient
import com.rarible.tzkt.client.SignatureValidationException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TezosSignatureTest {

    private val signatureClient: SignatureClient = mockk()
    private val tzktSignatureService = TzktSignatureServiceImpl(signatureClient)

    private val service = TezosSignatureService(tzktSignatureService)

    @Test
    fun `should validate signature successfully`() = runBlocking<Unit> {

        val publicKey = randomString()
        val signature = randomString()
        val message = randomString()

        coEvery { signatureClient.validate(publicKey, signature, message) } returns true

        assertTrue(service.validate(randomString(), publicKey, signature, message, null, null))
    }

    @Test
    fun `should validation throw exception`() = runBlocking<Unit> {

        val publicKey = randomString()
        val signature = randomString()
        val message = randomString()

        coEvery { signatureClient.validate(publicKey, signature, message) } throws SignatureValidationException("blablabla")

        assertThrows<UnionValidationException> {
            service.validate(randomString(), publicKey, signature, message, null, null)
        }
    }
}
