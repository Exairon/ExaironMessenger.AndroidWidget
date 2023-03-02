package com.exairon.widget.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.exairon.widget.model.FileUploadResponse
import com.exairon.widget.model.MessagesModel
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.repository.ChatActivityRepository.getMessagesServicesApiCall
import com.exairon.widget.repository.ChatActivityRepository.getWidgetSettingsServicesApiCall
import com.exairon.widget.repository.ChatActivityRepository.uploadFileServiceApiCall
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ChatActivityViewModel : ViewModel() {
    var widgetSettingsServicesLiveData: MutableLiveData<WidgetSettings?>? = null
    var messagesServicesLiveData: MutableLiveData<MessagesModel>? = null
    var uploadFileServicesLiveData: MutableLiveData<FileUploadResponse>? = null

    fun getWidgetSettings(channelId: String) : LiveData<WidgetSettings?>? {
        widgetSettingsServicesLiveData = getWidgetSettingsServicesApiCall(channelId)
        return widgetSettingsServicesLiveData
    }

    fun getNewMessages(timeStamp: String, conversationId: String): LiveData<MessagesModel>? {
        messagesServicesLiveData = getMessagesServicesApiCall(timeStamp, conversationId)
        return messagesServicesLiveData
    }

    fun uploadFileForChat(file: MultipartBody.Part, sessionId: String): LiveData<FileUploadResponse>? {
        uploadFileServicesLiveData = uploadFileServiceApiCall(file, sessionId)
        return uploadFileServicesLiveData
    }
}