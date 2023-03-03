package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
class Session(
    var conversationId: String? = null,
    var channelId: String? = null,
    var userToken: String? = null
)