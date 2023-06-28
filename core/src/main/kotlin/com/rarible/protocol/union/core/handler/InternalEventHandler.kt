package com.rarible.protocol.union.core.handler

import com.rarible.core.kafka.RaribleKafkaEventHandler

interface InternalEventHandler<B> : RaribleKafkaEventHandler<B>