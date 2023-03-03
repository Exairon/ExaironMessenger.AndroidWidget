package com.exairon.widget.model.widgetSettings

import androidx.annotation.Keep

@Keep
data class WidgetSettings(
    val `data`: Data?,
    val geo: Any?,
    val status: String?,
    val triggerRules: List<TriggerRule>?
) {
    companion object {
        @Volatile
        @JvmStatic
        private var INSTANCE: WidgetSettings? = null

        @JvmStatic
        @JvmOverloads
        fun getInstance(data: Data? = null, geo: Any? = null, status: String? = "", triggerRules: List<TriggerRule>? = null): WidgetSettings = INSTANCE
            ?: synchronized(this) {
                INSTANCE ?: WidgetSettings(data, geo, status, triggerRules).also { INSTANCE = it }
        }
    }
}