package com.exairon.widget.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.exairon.widget.model.MessagesModel
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.repository.ChatActivityRepository.getMessagesServicesApiCall
import com.exairon.widget.repository.ChatActivityRepository.getWidgetSettingsServicesApiCall

class ChatActivityViewModel : ViewModel() {
    var widgetSettingsServicesLiveData: MutableLiveData<WidgetSettings>? = null
    var messagesServicesLiveData: MutableLiveData<MessagesModel>? = null

    fun getWidgetSettings(channelId: String) : LiveData<WidgetSettings>? {
        widgetSettingsServicesLiveData = getWidgetSettingsServicesApiCall(channelId)
        return widgetSettingsServicesLiveData
    }

    fun getNewMessages(timeStamp: String, conversationId: String): LiveData<MessagesModel>? {
        messagesServicesLiveData = getMessagesServicesApiCall(timeStamp, conversationId)
        return messagesServicesLiveData
    }
}