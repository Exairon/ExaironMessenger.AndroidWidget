package com.exairon.widget.model

import androidx.annotation.Keep
import java.util.*
@Keep
class MessageTime(
    val day: String? = null,
    val hours: String? = null,
    val timestamp: String? = null,
    val timeObject: Date? = null
)