package com.rarible.protocol.union.core.service

import org.springframework.stereotype.Component

@Component
class ActivityServiceRouter(
    activityServices: List<ActivityService>
) : BlockchainRouter<ActivityService>(
    activityServices
)