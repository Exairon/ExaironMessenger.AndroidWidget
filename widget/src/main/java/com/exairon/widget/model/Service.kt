package com.exairon.widget.model

import androidx.annotation.Keep

@Keep
data class Service(
    val url: String? = null,
) {
    companion object {
        @Volatile
        @JvmStatic
        private var INSTANCE: Service? = null

        @JvmStatic
        @JvmOverloads
        fun getInstance(url: String? = ""): Service = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Service(url).also { INSTANCE = it }
        }
    }
}