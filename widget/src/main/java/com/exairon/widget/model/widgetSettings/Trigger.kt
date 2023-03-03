package com.exairon.widget.model.widgetSettings

import com.google.gson.annotations.SerializedName
data class Trigger (
    @SerializedName("when")
    val _when: String? = null,
    val timeOnPage__DISPLAYIF: Boolean? = null,
    val numberOfVisits__DISPLAYIF: Boolean? = null,
    val timeLimit__DISPLAYIF: Boolean? = null,
    val timeLimit: Number? = null,
    val url: List<Any>? = null
)