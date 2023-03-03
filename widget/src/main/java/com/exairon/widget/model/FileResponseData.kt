package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
class FileResponseData (
    val url: String? = null,
    val mimeType: String? = null,
    val originalname: String? = null,
    val attachmentId: String? = null,
    val size: String? = null,
)