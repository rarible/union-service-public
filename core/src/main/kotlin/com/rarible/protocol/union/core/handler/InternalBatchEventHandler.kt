package com.rarible.protocol.union.core.handler

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler

interface InternalBatchEventHandler<B> : RaribleKafkaBatchEventHandler<B>
