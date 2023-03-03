package com.exairon.widget.model.widgetSettings

import androidx.annotation.Keep

@Keep
data class Message(
    val headerMessage: String,
    val headerTitle: String,
    val isPopupIntent: Boolean,
    val lang: String,
    val placeholder: String,
    val popUpMessage: String,
    val popUpMessageDelay: Int,
    val popupIntent: String
)