package com.exairon.widget.retrofit

import com.exairon.widget.model.FileUploadResponse
import com.exairon.widget.model.MessagesModel
import com.exairon.widget.model.widgetSettings.WidgetSettings
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

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

    @Multipart
    @POST("/uploads/chat")
    fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("sessionId") sessionId: String
    ) : Call<FileUploadResponse>

}

