package com.exairon.widget.repository

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.exairon.widget.model.FileUploadResponse
import com.exairon.widget.model.MessagesModel
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.retrofit.RetrofitClient
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ChatActivityRepository {
    val widgetSettingsServiceSetterGetter = MutableLiveData<WidgetSettings?>()
    val messagesServiceSetterGetter = MutableLiveData<MessagesModel>()
    val uploadFileServiceSetterGetter = MutableLiveData<FileUploadResponse>()

    fun getWidgetSettingsServicesApiCall(channelId: String): MutableLiveData<WidgetSettings?> {

        val call = RetrofitClient.apiInterface.getWidgetSettings(channelId)

        call.enqueue(object: Callback<WidgetSettings> {
            override fun onFailure(call: Call<WidgetSettings>, t: Throwable) {
                // TODO("Not yet implemented")
                Log.v("DEBUG : ", t.message.toString())
            }

            override fun onResponse(
                call: Call<WidgetSettings>,
                response: Response<WidgetSettings>,
            ) {
                // TODO("Not yet implemented")
                Log.v("DEBUG : ", response.body().toString())
                try {
                    val data = response.body()

                    val msg = data!!

                    widgetSettingsServiceSetterGetter.value = msg
                } catch (e: Exception) {
                    widgetSettingsServiceSetterGetter.value = null
                }

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
                response: Response<MessagesModel>,
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

    fun uploadFileServiceApiCall(file: MultipartBody.Part, sessionId: String): MutableLiveData<FileUploadResponse> {

        val call = RetrofitClient.apiInterface.uploadFile(file, sessionId)

        call.enqueue(object: Callback<FileUploadResponse> {
            override fun onFailure(call: Call<FileUploadResponse>, t: Throwable) {
                // TODO("Not yet implemented")
                Log.v("DEBUG : ", t.message.toString())
                uploadFileServiceSetterGetter.value = FileUploadResponse("false")
            }

            override fun onResponse(
                call: Call<FileUploadResponse>,
                response: Response<FileUploadResponse>,
            ) {
                // TODO("Not yet implemented")
                Log.v("DEBUG : ", response.body().toString())

                val data = response.body()

                val msg = data!!

                uploadFileServiceSetterGetter.value = msg
            }
        })

        return uploadFileServiceSetterGetter
    }
}