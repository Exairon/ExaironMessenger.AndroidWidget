package com.exairon.widget.model.widgetSettings

data class FormElement(
    val field: String,
    val required: Boolean,
    val value: String? = null
)