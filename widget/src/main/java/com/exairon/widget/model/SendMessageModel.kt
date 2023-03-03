package com.exairon.widget.model

data class FileMessageModel(
    val document: String?,
    val mimeType: String?,
    val originalname: String?,
)
data class SendFileMessageModel(
    val channel_id: String,
    val message: FileMessageModel?,
    val session_id: String,
    val userToken: String,
    val user: User
)
data class LocationDataModel(
    val latitude: Double,
    val longitude: Double,
)

data class LocationMessageModel(
    val location: LocationDataModel
)
data class SendLocationMessageModel(
    val channel_id: String,
    val message: LocationMessageModel?,
    val session_id: String,
    val userToken: String,
    val user: User
)
data class SendMessageModel(
    val channel_id: String,
    val message: String?,
    val session_id: String,
    val userToken: String,
    val user: User
)