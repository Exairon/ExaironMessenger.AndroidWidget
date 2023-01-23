package com.exairon.widget.model

data class FileUploadResponse (
    var status: String,
    val data: FileResponseData? = null,
)