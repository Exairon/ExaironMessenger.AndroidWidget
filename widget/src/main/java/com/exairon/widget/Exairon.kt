package com.exairon.widget

import android.content.Context
import android.content.Intent
import com.exairon.widget.view.SplashActivity

object Exairon {
    var isActive = false
    var channelId: String? = null
    var name: String? = null
    var surname: String? = null
    var email: String? = null
    var phone: String? = null
    var src: String? = null
    var language = "tr"

    fun startChatActivity(context: Context?){
        if (channelId != null && src != null) {
            isActive = true
            val intent= Intent(context, SplashActivity::class.java)
            context?.startActivity(intent)
        }
    }

    fun init(c: Context) {
        if(isActive) startChatActivity(c)
    }
}