package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
class MessagesModel (
    var `data`: ArrayList<Message>? = null,
    var results: Int
    )