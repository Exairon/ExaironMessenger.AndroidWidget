package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
class Element (
        val image_url: String? = null,
        val subtitle: String? = null,
        val title: String? = null,
        val buttons: ArrayList<QuickReply>? = null
)