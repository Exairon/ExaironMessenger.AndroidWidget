package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
data class FileUploadResponse (
    var status: String,
    val data: FileResponseData? = null,
)