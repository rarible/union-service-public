package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.EventTimeMarksDto

data class ActivityEvent(
    val activity: ActivityDto,
    val eventTimeMarks: EventTimeMarksDto?
)
