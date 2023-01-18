package com.exairon.widget.model.widgetSettings

data class FormFields(
    val emailFieldRequired: Boolean,
    val nameFieldRequired: Boolean,
    val phoneFieldRequired: Boolean,
    val showEmailField: Boolean,
    val showNameField: Boolean,
    val showPhoneField: Boolean,
    val showSurnameField: Boolean,
    val surnameFieldRequired: Boolean
)