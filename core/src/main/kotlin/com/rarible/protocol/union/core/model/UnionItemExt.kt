package com.rarible.protocol.union.core.model

import java.math.BigInteger

val UnionItem.loadMetaSynchronously: Boolean
    /**
     * We need to load meta synchronously for Lazy items and just minted item in pending state
     */
    get() = lazySupply > BigInteger.ZERO ||
            (supply == BigInteger.ZERO && pending.isNotEmpty())