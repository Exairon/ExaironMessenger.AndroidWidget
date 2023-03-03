package com.exairon.widget.model.widgetSettings

import androidx.annotation.Keep

@Keep
data class TriggerRule (
    val trigger: Trigger? = null,
    val enabled: Boolean? = null,
    val triggerType: String? = null,
    val name: String? = null,
    val text: String? = null,
    val payload: String? = null,
    val description: String? = null
)