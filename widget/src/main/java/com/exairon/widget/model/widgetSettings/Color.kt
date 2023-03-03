package com.exairon.widget.model.widgetSettings

import androidx.annotation.Keep

@Keep
data class Color(
    val botMessageBackColor: String,
    val botMessageFontColor: String,
    val buttonBackColor: String,
    val buttonFontColor: String,
    val headerColor: String,
    val headerFontColor: String,
    val userMessageBackColor: String,
    val userMessageFontColor: String
)