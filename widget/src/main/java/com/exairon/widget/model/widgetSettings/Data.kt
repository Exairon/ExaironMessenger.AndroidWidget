package com.exairon.widget.model.widgetSettings

data class Data(
    val __v: Int,
    val _id: String,
    val avatar: String,
    val color: Color,
    val created: String,
    val deleted: Boolean,
    val editable: Boolean,
    val font: String,
    val formFields: FormFields,
    val hideIcon: Boolean,
    val icon: String,
    val lastUpdated: String,
    val licenseAgreementLink: String,
    val messages: List<Message>,
    val mobilePosition: String,
    val name: String,
    val popUpMessageDelay: Int,
    val popUpMessageType: String,
    val position: String,
    val showAttachments: Boolean,
    val showSurvey: Boolean,
    val showUserForm: Boolean,
    val whiteLabelWidget: Boolean
)