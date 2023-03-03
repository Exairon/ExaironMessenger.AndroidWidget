package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
class SurveyRequest (
    var channelId: String? = null,
    var session_id: String? = null,
    var surveyResult: SurveyResult? = null,
)