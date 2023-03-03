package com.exairon.widget.model

class SessionRequest(
    val session_id: String? = null,
    val channel_id: String? = null
) {
    companion object {
        @Volatile
        @JvmStatic
        private var INSTANCE: SessionRequest? = null

        @JvmStatic
        @JvmOverloads
        fun getInstance(session_id: String? = "", channel_id: String? = ""): SessionRequest = INSTANCE ?: synchronized(this) {
            INSTANCE ?: SessionRequest(session_id, channel_id).also { INSTANCE = it }
        }
    }
}