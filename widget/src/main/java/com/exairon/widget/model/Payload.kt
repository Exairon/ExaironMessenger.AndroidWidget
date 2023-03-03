package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
class Payload (
    var src: String? = null,
    var videoType: String? = null,
    var mimeType: String? = null,
    var originalname: String? = null,
    var elements: ArrayList<Element>? = null
)