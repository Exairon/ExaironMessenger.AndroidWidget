package com.exairon.widget.retrofit

import com.exairon.widget.model.MessagesModel
import com.exairon.widget.model.widgetSettings.WidgetSettings
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiInterface {
    @GET("/api/v1/channels/widgetSettings/{channelId}")
    fun getWidgetSettings(
        @Path("channelId") channelId: String
    ) : Call<WidgetSettings>

    @GET("/api/v1/messages/getNewMessages/{timestamp}/{conversationId}")
    fun getNewMessages(
        @Path("timestamp") timestamp: String,
        @Path("conversationId") conversationId: String
    ) : Call<MessagesModel>
}