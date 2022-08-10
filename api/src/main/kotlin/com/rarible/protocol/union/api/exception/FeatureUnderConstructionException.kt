package com.rarible.protocol.union.api.exception

import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(value = org.springframework.http.HttpStatus.NOT_IMPLEMENTED)
class FeatureUnderConstructionException(message: String) : RuntimeException(message)