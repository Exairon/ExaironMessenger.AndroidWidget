package com.exairon.widget
import com.exairon.widget.model.Session
import com.google.android.gms.maps.model.LatLng

object StateManager {
    var oldSessionId: String? = null
    var tempSession: Session? = null
    var conversationId: String? = null
    var channelId: String? = null
    var userToken: String? = null
    var location: LatLng? = null
}