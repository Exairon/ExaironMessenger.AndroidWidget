package com.exairon.widget.model.widgetSettings

import androidx.annotation.Keep

@Keep
data class FormElement(
    val field: String,
    val required: Boolean,
    val value: String? = null
)