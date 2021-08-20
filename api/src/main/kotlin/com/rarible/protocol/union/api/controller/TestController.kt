package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.dto.TestDto
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class TestController : TestControllerApi {

    override suspend fun getTest(): ResponseEntity<TestDto> {
        return ResponseEntity.ok(TestDto(12))
    }
}