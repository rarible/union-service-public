package com.rarible.protocol.union.core.misc

import scalether.domain.Address
import java.math.BigInteger

fun toItemId(contract: Address, tokenId: BigInteger) = "$contract:$tokenId"
