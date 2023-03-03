package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
class Attachment(
    var type: String? = null,
    var payload: Payload? = null
)