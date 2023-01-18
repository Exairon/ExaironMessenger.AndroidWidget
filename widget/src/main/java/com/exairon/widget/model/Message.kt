package com.exairon.widget.model

data class Message(
    var id: String? = null,
    val text: String? = null,
    val fromCustomer: Boolean? = null,
    val type: String? = null,
    var time: MessageTime? = null,
    val message: String? = null,
    val custom: Custom? = null,
    val attachment: Attachment? = null,
    val quick_replies: ArrayList<QuickReply>? = null,
    val ruleMessage: Boolean? = null
)