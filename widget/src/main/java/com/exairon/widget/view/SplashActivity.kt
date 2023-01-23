package com.exairon.widget.view
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.exairon.widget.Exairon
import com.exairon.widget.R
import com.exairon.widget.StateManager
import com.exairon.widget.model.*
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.socket.SocketHandler
import com.exairon.widget.viewmodel.ChatActivityViewModel
import com.google.gson.Gson
import io.socket.client.Socket
import org.json.JSONObject
import org.w3c.dom.Document
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    var widgetSettings: WidgetSettings? = null
    var launchedActivity = false
    lateinit var context: Context
    private lateinit var mSocket: Socket

    private fun writeSessionInfo(session: Session) {
        val xmlString = "<root>" +
                "<sessionId>${session.conversationId}</sessionId>" +
                "<channelId>${session.channelId}</channelId>" +
                "<userToken>${session.userToken}</userToken>" +
                "</root>"
        val file = File(context.getExternalFilesDir(null), "session.xml")
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(xmlString.toByteArray())
        fileOutputStream.close()
    }

    private fun getSessionInfo(): Session {
        val channelId = StateManager.channelId
        val conversationId = StateManager.conversationId
        val userToken = StateManager.userToken
        return Session(conversationId = conversationId, channelId = channelId, userToken = userToken)
    }

    private fun readSessionInfo(): Session {
        val channelId = Exairon.channelId ?: getSessionInfo().channelId.toString()
        try {
            val file = File(context.getExternalFilesDir(null), "session.xml")
            val fileInputStream = FileInputStream(file)
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document: Document = documentBuilder.parse(fileInputStream)
            val rootElement = document.documentElement

            val sessionElements = rootElement.getElementsByTagName("sessionId")
            val tokenElements = rootElement.getElementsByTagName("userToken")

            val sessionElement = sessionElements.item(0)
            val tokenElement = tokenElements.item(0)

            val sessionId = sessionElement.textContent
            val userToken = tokenElement.textContent

            return Session(sessionId, channelId, userToken)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
        return Session(channelId= channelId)
    }

    private fun readUserInfo(): User? {
        try {
            val file = File(context.getExternalFilesDir(null), "user.xml")
            val fileInputStream = FileInputStream(file)
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document: Document = documentBuilder.parse(fileInputStream)
            val rootElement = document.documentElement

            val nameElements = rootElement.getElementsByTagName("name")
            val surnameElements = rootElement.getElementsByTagName("surname")
            val emailElements = rootElement.getElementsByTagName("email")
            val phoneElements = rootElement.getElementsByTagName("phone")

            val nameElement = nameElements.item(0)
            val surnameElement = surnameElements.item(0)
            val emailElement = emailElements.item(0)
            val phoneElement = phoneElements.item(0)

            val name = nameElement.textContent
            val surname = surnameElement.textContent
            val email = emailElement.textContent
            val phone = phoneElement.textContent

            return User(email=email, name=name, phone=phone, surname=surname)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        context = this@SplashActivity

        val chatActivityViewModel = ViewModelProvider(this)[ChatActivityViewModel::class.java]

        val channelIdParams: String
        val userName: String?
        val userSurname: String?
        val userEmail: String?
        val userPhone: String?
        var isActive = false

        if (Exairon.channelId != null) {
            channelIdParams = Exairon.channelId!!
            userName = Exairon.name
            userSurname = Exairon.surname!!
            userEmail = Exairon.email!!
            userPhone = Exairon.phone!!
            Service.getInstance(Exairon.src!!)
        } else {
            val session = getSessionInfo()
            val user = User.getInstance()
            channelIdParams = session.channelId.toString()
            userName = user.name
            userSurname = user.surname
            userEmail = user.email
            userPhone = user.phone
        }

        val session = readSessionInfo()
        val user = readUserInfo()

        mSocket = SocketHandler.setSocket()
        mSocket.connect()

        val req = if (session.conversationId != null || session.conversationId !== "") {
            StateManager.oldSessionId = session.conversationId
            SessionRequest(session.conversationId, session.channelId)
        } else {
            SessionRequest(channel_id = Exairon.channelId)
        }

        mSocket.on("session_confirm") { args ->
            if (args[0] != null) {
                val convId = args[0] as String
                val userToken = if (session.userToken == null || session.userToken == "") {
                    UUID.randomUUID().toString()
                } else {
                    session.userToken.toString()
                }
                val session = Session(convId, req.channel_id, userToken)
                if (!launchedActivity) {
                    launchedActivity = true
                    val widgetSettingsData = WidgetSettings.getInstance()
                    val formFields = widgetSettingsData.data?.formFields
                    if ((req.session_id == null || req.session_id == "") &&
                        (widgetSettingsData.data?.showUserForm!! &&
                                formFields?.showNameField!! && userName == null && user?.name == null ||
                                formFields?.showSurnameField!! && userSurname == null && user?.surname == null ||
                                formFields.showEmailField && userEmail == null && user?.email == null ||
                                formFields.showPhoneField && userPhone == null && user?.phone == null)
                    ) {
                        InitialUser.getInstance(userEmail, userName, userPhone, userSurname)
                        StateManager.tempSession = Session(
                            conversationId = convId,
                            channelId = req.channel_id,
                            userToken = userToken
                        )
                        val intent = Intent(this, FormActivity::class.java)
                        startActivity(intent)
                    } else {
                        User.getInstance(
                            name = user?.name ?: userName,
                            surname = user?.surname ?: userSurname,
                            email = user?.email ?: userEmail,
                            phone = user?.phone ?: userPhone)
                        if (req.session_id == null || req.session_id == "") {
                            writeSessionInfo(session)
                        }
                        if (Exairon.channelId != session.channelId.toString() && req.channel_id != null) {
                            StateManager.channelId = req.channel_id
                            StateManager.conversationId = convId
                            StateManager.userToken = userToken
                        } else {
                            StateManager.channelId = session.channelId.toString()
                            StateManager.conversationId = convId
                            StateManager.userToken = userToken
                        }
                        val intent = Intent(this, ChatActivity::class.java)
                        startActivity(intent)
                    }
                }
            }
        }

        chatActivityViewModel.getWidgetSettings(channelIdParams)!!.observe(this, Observer { widgetSettingsData ->
            WidgetSettings.getInstance(widgetSettingsData.data, widgetSettingsData.geo, widgetSettingsData.status, widgetSettingsData.triggerRules)

            val botMessageDrawable = AppCompatResources.getDrawable(context, R.drawable.rounded_corner_customer)
            val userMessageDrawable = AppCompatResources.getDrawable(context, R.drawable.rounded_corner_bot)
            val closeChatDrawable = AppCompatResources.getDrawable(context, R.drawable.close_chat)
            val closeSessionDrawable = AppCompatResources.getDrawable(context, R.drawable.close_session)
            val backButtonDrawable = AppCompatResources.getDrawable(context, R.drawable.back)
            val buttonBackgroundDrawable = AppCompatResources.getDrawable(context, R.drawable.button_background_color)

            val botWrappedDrawable = DrawableCompat.wrap(botMessageDrawable!!)
            val userWrappedDrawable = DrawableCompat.wrap(userMessageDrawable!!)
            val closeChatWrappedDrawable = DrawableCompat.wrap(closeChatDrawable!!)
            val closeSessionWrappedDrawable = DrawableCompat.wrap(closeSessionDrawable!!)
            val backButtonWrappedDrawable = DrawableCompat.wrap(backButtonDrawable!!)
            val buttonBackgroundWrappedDrawable = DrawableCompat.wrap(buttonBackgroundDrawable!!)

            DrawableCompat.setTint(botWrappedDrawable, Color.parseColor(widgetSettingsData.data?.color?.botMessageBackColor))
            DrawableCompat.setTint(userWrappedDrawable, Color.parseColor(widgetSettingsData.data?.color?.userMessageBackColor))
            DrawableCompat.setTint(closeChatWrappedDrawable, Color.parseColor(widgetSettingsData.data?.color?.headerFontColor))
            DrawableCompat.setTint(closeSessionWrappedDrawable, Color.parseColor(widgetSettingsData.data?.color?.headerFontColor))
            DrawableCompat.setTint(backButtonWrappedDrawable, Color.parseColor(widgetSettingsData.data?.color?.headerFontColor))
            DrawableCompat.setTint(buttonBackgroundWrappedDrawable, Color.parseColor(widgetSettingsData.data?.color?.buttonBackColor))

            mSocket.emit("session_request", JSONObject(Gson().toJson(req)) )
        })
    }

    override fun onDestroy() {
        mSocket.off("session_confirm")
        super.onDestroy()
    }
}