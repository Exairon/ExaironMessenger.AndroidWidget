package com.exairon.widget.model

data class SendMessageModel(
    val channel_id: String,
    val message: String,
    val session_id: String,
    val userToken: String,
    val user: User
)