package com.exairon.widget.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.exairon.widget.model.MessagesModel
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.retrofit.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ChatActivityRepository {
    val widgetSettingsServiceSetterGetter = MutableLiveData<WidgetSettings>()
    val messagesServiceSetterGetter = MutableLiveData<MessagesModel>()

    fun getWidgetSettingsServicesApiCall(channelId: String): MutableLiveData<WidgetSettings> {

        val call = RetrofitClient.apiInterface.getWidgetSettings(channelId)

        call.enqueue(object: Callback<WidgetSettings> {
            override fun onFailure(call: Call<WidgetSettings>, t: Throwable) {
                // TODO("Not yet implemented")
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<WidgetSettings>,
                response: Response<WidgetSettings>
            ) {
                // TODO("Not yet implemented")
                Log.v("DEBUG : ", response.body().toString())

                val data = response.body()

                val msg = data!!

                widgetSettingsServiceSetterGetter.value = msg
            }
        })

        return widgetSettingsServiceSetterGetter
    }

    fun getMessagesServicesApiCall(timeStamp: String, conversationId: String): MutableLiveData<MessagesModel> {

        val call = RetrofitClient.apiInterface.getNewMessages(timeStamp, conversationId)

        call.enqueue(object: Callback<MessagesModel> {
            override fun onFailure(call: Call<MessagesModel>, t: Throwable) {
                // TODO("Not yet implemented")
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<MessagesModel>,
                response: Response<MessagesModel>
            ) {
                // TODO("Not yet implemented")
                Log.v("DEBUG : ", response.body().toString())

                val data = response.body()

                val msg = data!!

                messagesServiceSetterGetter.value = msg
            }
        })

        return messagesServiceSetterGetter
    }
}