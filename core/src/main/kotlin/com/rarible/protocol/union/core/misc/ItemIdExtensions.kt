package com.rarible.protocol.union.core.misc

import scalether.domain.Address
import java.math.BigInteger

fun toItemId(contract: String, tokenId: String) = "$contract:$tokenId"
fun toItemId(contract: String, tokenId: Int) = "$contract:$tokenId"
fun toItemId(contract: Address, tokenId: BigInteger) = "$contract:$tokenId"
