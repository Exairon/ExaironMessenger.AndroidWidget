package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
class QuickReply (
    val content_type: String? = null,
    val payload: String? = null,
    val title: String? = null,
    val type: String? = null,
    val url: String? = null
)