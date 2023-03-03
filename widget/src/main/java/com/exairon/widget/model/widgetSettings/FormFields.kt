package com.exairon.widget.model.widgetSettings

import androidx.annotation.Keep

@Keep
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