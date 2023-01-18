package com.exairon.widget
import com.exairon.widget.model.Session

object StateManager {
    var oldSessionId: String? = null
    var tempSession: Session? = null
    var conversationId: String? = null
    var channelId: String? = null
    var userToken: String? = null
}