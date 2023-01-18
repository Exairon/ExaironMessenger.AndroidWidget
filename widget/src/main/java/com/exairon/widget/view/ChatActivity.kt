package com.exairon.widget.view

import android.Manifest
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.Observer
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.exairon.widget.Exairon
import com.exairon.widget.R
import com.exairon.widget.StateManager
import com.exairon.widget.adaptor.MessageAdapter
import com.exairon.widget.databinding.ActivityChatBinding
import com.exairon.widget.model.*
import com.exairon.widget.model.Message
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.socket.SocketHandler
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.document_dialog.*
import kotlinx.android.synthetic.main.text_message_bot.*
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayList
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.ViewModelProvider
import com.exairon.widget.viewmodel.ChatActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.collections.HashMap

class ChatActivity : AppCompatActivity() {

    lateinit var context: Context
    lateinit var binding: ActivityChatBinding
    private val STORAGE_PERMISSION_CODE: Int = 1000
    var itemDownload : Long = 0
    private lateinit var fileSrc: String
    lateinit var fileName: String
    var pickedPhoto : Uri? = null
    var pickedBitMap : Bitmap? = null
    private val mSocket = SocketHandler.getSocket()

    private fun writeUserInfo(user: User) {
        val xmlString = "<root>" +
                "<name>${user.name}</name>" +
                "<surname>${user.surname}</surname>" +
                "<email>${user.email}</email>" +
                "<phone>${user.phone}</phone>" +
                "</root>"
        val file = File(context.getExternalFilesDir(null), "user.xml")
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(xmlString.toByteArray())
        fileOutputStream.close()
    }

    private fun setWidgetProperties(widgetSettings: WidgetSettings) {
        // Data
        val messages = if (widgetSettings.data?.messages?.size == 1) {
            widgetSettings.data.messages[0]
        } else {
            var currentIndex = 0
            widgetSettings.data?.messages?.forEachIndexed { index, message ->
                if (message.lang == Exairon.language) {
                    currentIndex = index
                }
            }
            widgetSettings.data?.messages?.get(currentIndex)
        }
        val colors = widgetSettings.data?.color

        // Views
        val topBorView = findViewById<ConstraintLayout>(R.id.top_bar)
        val greetingTitleView = findViewById<TextView>(R.id.greeting_tite)
        val greetingMessageView = findViewById<TextView>(R.id.greeting_message)
        val editTextView = findViewById<EditText>(R.id.chatSender)

        topBorView.setBackgroundColor(Color.parseColor(colors?.headerColor))

        greetingTitleView.text = messages?.headerTitle
        greetingTitleView.setTextColor(Color.parseColor(colors?.headerFontColor))

        greetingMessageView.text = messages?.headerMessage
        greetingMessageView.setTextColor(Color.parseColor(colors?.headerFontColor))

        editTextView.hint = messages?.placeholder

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

        DrawableCompat.setTint(botWrappedDrawable, Color.parseColor(widgetSettings.data?.color?.botMessageBackColor))
        DrawableCompat.setTint(userWrappedDrawable, Color.parseColor(widgetSettings.data?.color?.userMessageBackColor))
        DrawableCompat.setTint(closeChatWrappedDrawable, Color.parseColor(widgetSettings.data?.color?.headerFontColor))
        DrawableCompat.setTint(closeSessionWrappedDrawable, Color.parseColor(widgetSettings.data?.color?.headerFontColor))
        DrawableCompat.setTint(backButtonWrappedDrawable, Color.parseColor(widgetSettings.data?.color?.headerFontColor))
        DrawableCompat.setTint(buttonBackgroundWrappedDrawable, Color.parseColor(widgetSettings.data?.color?.buttonBackColor))
    }

    private fun startDownloading() {
        val request = DownloadManager.Request(Uri.parse(fileSrc))
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE )
        request.setTitle("Download")
        request.setDescription("The file is Downloading...")

        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)


        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        this.itemDownload = manager.enqueue(request)

        val br = object: BroadcastReceiver(){
            override fun onReceive(p0: Context?, p1: Intent?) {
                val id = p1?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if(id==itemDownload){
                    Toast.makeText(applicationContext, "Download Completed.", Toast.LENGTH_LONG).show()
                }
            }
        }
        registerReceiver(br, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun clearMessages() {
        val xmlString = "<root></root>"
        val file = File(context.getExternalFilesDir(null), "messages.xml")
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(xmlString.toByteArray())
        fileOutputStream.close()
    }

    private fun writeMessage(message: Message) {
        val file = File(context.getExternalFilesDir(null), "messages.xml")
        val fileLast = File(context.getExternalFilesDir(null), "lastMessage.xml")
        var text = ""
        try {
            val fileInputStream = FileInputStream(file)
            text = fileInputStream.reader().use { reader ->
                reader.readText()
            }
        } catch (ex: Exception) {
            // Error
        }
        val fileOutputStream = FileOutputStream(file)
        var oldMessages = text.replace("<root>", "").replace("</root>", "")
        if (oldMessages.contains("<type>survey</type")) oldMessages = ""
        var xmlString = "<root>" +
                oldMessages +
                "<message>" +
                "<time>" +
                "<day>${message.time?.day ?: ""}</day>" +
                "<hours>${message.time?.hours ?: ""}</hours>" +
                "<timestamp>${message.time?.timestamp ?: ""}</timestamp>" +
                "</time>" +
                "<fromCustomer>${message.fromCustomer ?: ""}</fromCustomer>" +
                "<rule>${message.ruleMessage ?: ""}</rule>" +
                "<type>${message.type}</type>"
        when(message.type) {
            "text" -> xmlString += "<text>${message.text?.replace("<", "&lt;")}</text>"
            "image" -> xmlString += "<src>${message.attachment?.payload?.src?.replace("<", "&lt;")}</src>"
            "button" -> {
                xmlString += "<title>${message.text?.replace("<", "&lt;")}</title>" +
                        "<buttons>"
                for (button in message.quick_replies!!) {
                    xmlString += "<button>" +
                            "<type>${button.type}</type>" +
                            "<url>${button.url?.replace("<", "&lt;")}</url>" +
                            "<payload>${button.payload}</payload>" +
                            "<title>${button.title?.replace("<", "&lt;")}</title>" +
                            "</button>"
                }
                xmlString += "</buttons>"
            }
            "video" -> {
                xmlString += "<videoType>${message.attachment?.payload?.videoType}</videoType>" +
                        "<src>${message.attachment?.payload?.src?.replace("<", "&lt;")}</src>"
            }
            "audio" -> xmlString += "<src>${message.custom?.data?.attachment?.payload?.src}</src>"
            "document" -> {
                val payload = message.custom?.data?.attachment?.payload
                xmlString += "<src>${payload?.src?.replace("<", "&lt;")}</src>" +
                        "<originalname>${payload?.originalname?.replace("<", "&lt;")}</originalname>"
            }
            "carousel" -> {
                xmlString += "<cards>"
                for (card in message.attachment?.payload?.elements!!) {
                    xmlString += "<card><title>${card.title?.replace("<", "&lt;")}</title>" +
                            "<subtitle>${card.subtitle?.replace("<", "&lt;")}</subtitle>" +
                            "<imageUrl>${card.image_url?.replace("<", "&lt;")}</imageUrl>" +
                            "<buttons>"
                    for (button in card.buttons!!) {
                        xmlString += "<button>" +
                                "<type>${button.type}</type>" +
                                "<url>${button.url?.replace("<", "&lt;")}</url>" +
                                "<payload>${button.payload}</payload>" +
                                "<title>${button.title?.replace("<", "&lt;")}</title>" +
                                "</button>"
                    }
                    xmlString += "</buttons></card>"
                }
                xmlString += "</cards>"
            }
            "survey" -> xmlString += "<survey>true</survey>"

        }
        xmlString += "</message></root>"
        val fileLastOutputStream = FileOutputStream(fileLast)
        val time = if (message.fromCustomer == true) {
            message.time?.timestamp
        } else {
            message.time?.timestamp?.toString()
        }
        val lastString = "<root><lastMessageTime>${time ?: ""}</lastMessageTime></root>"

        fileOutputStream.write(xmlString.toByteArray())
        fileLastOutputStream.write(lastString.toByteArray())

        fileOutputStream.close()
        fileLastOutputStream.close()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun readMessage(): ArrayList<Message> {
        try {
            val file = File(context.getExternalFilesDir(null), "messages.xml")
            val fileInputStream = FileInputStream(file)

            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document: Document = documentBuilder.parse(fileInputStream)
            val rootElement = document.documentElement

            val messageList = ArrayList<Message>()

            val messageRoots = rootElement.getElementsByTagName("message")
            for (i in 0 until messageRoots.length) {
                if (messageRoots.item(i).nodeType == Node.ELEMENT_NODE) {
                    val element = messageRoots.item(i) as Element
                    val messageType = getNodeValue("type", element)
                    val fromCustomer = getNodeValue(
                        "fromCustomer",
                        element
                    ) == "true"
                    val rule = getNodeValue("rule", element) == "true"
                    val messageTimeElements = element.getElementsByTagName("time")
                    val messageTime = if (messageTimeElements?.item(0) != null) {
                        val messageTimeElement = messageTimeElements.item(0) as Element
                        val messageDay = getNodeValue("day", messageTimeElement)
                        val messageHours = getNodeValue("hours", messageTimeElement)
                        val messageTimestamp = getNodeValue("timestamp", messageTimeElement)
                        MessageTime(day = messageDay, hours = messageHours, timestamp = messageTimestamp)
                    } else {
                        MessageTime()
                    }

                    var message = Message()
                    when(messageType) {
                        "text" -> {
                            val text = getNodeValue("text", element).replace("&lt;", "<")
                            message = Message(
                                text = text, fromCustomer = fromCustomer, type = messageType, time = messageTime, ruleMessage = rule)
                        }
                        "image" -> {
                            val src = getNodeValue("src", element).replace("&lt;", "<")
                            val payload = Payload(src = src)
                            val attachment = Attachment(payload = payload)
                            message = Message(
                                fromCustomer = fromCustomer, type = messageType, attachment = attachment, time = messageTime, ruleMessage = rule)
                        }
                        "button" -> {
                            val title = getNodeValue("title", element).replace("&lt;", "<")
                            val buttonsRoot = element.getElementsByTagName("buttons").item(0) as Element
                            val buttonRoots = buttonsRoot.getElementsByTagName("button")
                            val buttonList = ArrayList<QuickReply>()
                            for (j in 0 until buttonRoots.length) {
                                if (buttonRoots.item(j).nodeType == Node.ELEMENT_NODE) {
                                    val buttonElement = buttonRoots.item(j) as Element
                                    val buttonType = getNodeValue("type", buttonElement)
                                    val url = getNodeValue("url", buttonElement).replace("&lt;", "<")
                                    val payload = getNodeValue("payload", buttonElement)
                                    val buttonTitle = getNodeValue("title", buttonElement).replace("&lt;", "<")
                                    val button = QuickReply(
                                        payload = payload, title = buttonTitle, url = url, type = buttonType
                                    )
                                    buttonList.add(button)
                                }
                            }
                            message = Message(text = title, type = messageType, quick_replies = buttonList, time = messageTime, ruleMessage = rule)
                        }
                        "video" -> {
                            val videoType = getNodeValue("videoType", element)
                            val src = getNodeValue("src", element).replace("&lt;", "<")
                            val payload = Payload(videoType = videoType, src = src)
                            val attachment = Attachment(payload = payload)
                            message = Message(type = messageType, attachment = attachment, time = messageTime, ruleMessage = rule)
                        }
                        "audio" -> {
                            val src = getNodeValue("src", element).replace("&lt;", "<")
                            val payload = Payload(src = src)
                            val attachment = Attachment(payload = payload)
                            val data = CustomData(attachment = attachment)
                            val custom = Custom(data = data)
                            message = Message(type = messageType, custom = custom, time = messageTime, ruleMessage = rule)
                        }
                        "document" -> {
                            val originalName = getNodeValue("originalname", element).replace("&lt;", "<")
                            val originalSrc = getNodeValue("src", element).replace("&lt;", "<")
                            val payload = Payload(src = originalSrc, originalname = originalName)
                            val attachment = Attachment(payload = payload)
                            val data = CustomData(attachment = attachment)
                            val custom = Custom(data = data)
                            message = Message(type = messageType, custom = custom, time = messageTime, ruleMessage = rule)
                        }
                        "carousel" -> {
                            val cards = ArrayList<com.exairon.widget.model.Element>()
                            val cardsRoot = element.getElementsByTagName("cards").item(0) as Element
                            val cardRoots = cardsRoot.getElementsByTagName("card")
                            for (j in 0 until cardRoots.length) {
                                if (cardRoots.item(j).nodeType == Node.ELEMENT_NODE) {
                                    val cardElement = cardRoots.item(j) as Element
                                    val cardTitle = getNodeValue("title", cardElement).replace("&lt;", "<")
                                    val cardSubTitle = getNodeValue("subtitle", cardElement).replace("&lt;", "<")
                                    val cardImageUrl = getNodeValue("imageUrl", cardElement).replace("&lt;", "<")

                                    val buttonsRoot = cardElement.getElementsByTagName("buttons").item(0) as Element
                                    val buttonRoots = buttonsRoot.getElementsByTagName("button")
                                    val buttonList = ArrayList<QuickReply>()
                                    for (k in 0 until buttonRoots.length) {
                                        if (buttonRoots.item(k).nodeType == Node.ELEMENT_NODE) {
                                            val buttonElement = buttonRoots.item(k) as Element
                                            val buttonType = getNodeValue("type", buttonElement)
                                            val url = getNodeValue("url", buttonElement).replace("&lt;", "<")
                                            val payload = getNodeValue("payload", buttonElement)
                                            val buttonTitle = getNodeValue("title", buttonElement).replace("&lt;", "<")
                                            val button = QuickReply(
                                                payload = payload, title = buttonTitle, url = url, type = buttonType
                                            )
                                            buttonList.add(button)
                                        }
                                    }
                                    val card = Element(
                                        image_url = cardImageUrl, title = cardTitle,
                                        subtitle = cardSubTitle, buttons = buttonList
                                    )
                                    cards.add(card)
                                }
                            }
                            val payload = Payload(elements = cards)
                            val attachment = Attachment(payload = payload)
                            message = Message(attachment = attachment, type = messageType, time = messageTime, ruleMessage = rule)
                        }
                        "survey" -> {
                            close_session.visibility = View.GONE
                            chatSendArea.visibility = View.GONE
                            message = Message(type = messageType)
                        }

                    }
                    message.id = UUID.randomUUID().toString()
                    messageList.add(message)
                }
            }
            fileInputStream.close()
            return messageList
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
        clearMessages()
        return ArrayList<Message>()
    }

    private fun getNodeValue(tag: String, element: Element): String {
        val nodeList = element.getElementsByTagName(tag)
        val node = nodeList.item(0)
        if (node != null) {
            if (node.hasChildNodes()) {
                val child = node.firstChild
                while (child != null) {
                    if (child.nodeType === Node.TEXT_NODE) {
                        return child.nodeValue
                    }
                }
            }
        }
        return ""
    }

    private fun getSessionInfo(): Session {
        val channelId = StateManager.channelId
        val conversationId = StateManager.conversationId
        val userToken = StateManager.userToken
        return Session(conversationId = conversationId, channelId = channelId, userToken = userToken)
    }

    private fun clearSessionInfo() {
        val session = getSessionInfo()
        if (session.channelId != null && session.userToken != null) {
            StateManager.conversationId = ""
            StateManager.channelId = session.channelId!!
            StateManager.userToken = session.userToken!!
        }
        val xmlString = "<root>" +
                "<sessionId></sessionId>" +
                "<channelId>${session.channelId}</channelId>" +
                "<userToken>${session.userToken}</userToken>" +
                "</root>"
        val file = File(context.getExternalFilesDir(null), "session.xml")
        val fileOutputStream = FileOutputStream(file)
        fileOutputStream.write(xmlString.toByteArray())
        fileOutputStream.close()
    }

    fun checkPermission(fileSrc: String, name: String) {
        this.fileSrc = fileSrc
        fileName = name
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
            }
            else{
                startDownloading()
            }
        }
        else{
            startDownloading()
        }
    }

    private fun getLastMessageTime(): String? {
        try {
            val file = File(context.getExternalFilesDir(null), "lastMessage.xml")
            val fileInputStream = FileInputStream(file)
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val document: Document = documentBuilder.parse(fileInputStream)
            val rootElement = document.documentElement

            val lastMessageElements = rootElement.getElementsByTagName("lastMessageTime")
            val lastMessageElement = lastMessageElements.item(0)

            return lastMessageElement.textContent
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        }
        return null
    }

    private fun showSurvey(): Message {
        val message = Message(id = UUID.randomUUID().toString(), type = "survey")
        writeMessage(message)
        if (close_session.visibility === View.VISIBLE) {
            close_session.visibility = View.GONE
        }
        if (chatSendArea.visibility === View.VISIBLE){
            chatSendArea.visibility = View.GONE
        }
        return message
    }

    private val pickImage = 100
    private var imageUri: Uri? = null

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this@ChatActivity
        val chatActivityViewModel = ViewModelProvider(this)[ChatActivityViewModel::class.java]

        val config = resources.configuration
        val lang = Exairon.language // your language code
        val locale = Locale(lang)
        Locale.setDefault(locale)
        config.setLocale(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)

        this.setContentView(R.layout.activity_chat)

        val oldSessionId = StateManager.oldSessionId

        val session = getSessionInfo()
        val user = User.getInstance()
        val widgetSettings = WidgetSettings.getInstance()
        var previousMessages = ArrayList<Message>()

        val messages = ArrayList<Message>()

        val messageList: HashMap<String, View> = HashMap<String, View>()
        val messageAdapter = MessageAdapter(
            this,
            R.layout.text_message_bot,
            messages,
            messageList,
            widgetSettings
        )

        val lastMessageTime = getLastMessageTime()

        if (lastMessageTime != null && lastMessageTime.count() > 0 &&
            session.conversationId != null && session.conversationId!!.count() > 0) {
            chatActivityViewModel.getNewMessages(
                lastMessageTime.toString(),
                session.conversationId.toString()
            )!!.observe(this, Observer { messages ->
                if (messages.results > 0 && messages?.data != null) {
                    for (message in messages.data!!) {
                        if (message.time?.timeObject != null) {
                            val hours = message.time?.timeObject?.hours
                            val minutes = message.time?.timeObject?.minutes
                            val date = message.time?.timeObject?.date
                            val month = message.time?.timeObject?.month?.plus(1)
                            val year = message.time?.timeObject?.year?.plus(1900)
                            val hoursString =
                                if (hours!! < 10) "0$hours"
                                else "$hours"
                            val minutesString =
                                if (minutes!! < 10) "0$minutes"
                                else "$minutes"
                            val dateString =
                                if (date!! < 10) "0$date"
                                else "$date"

                            val messageTime = MessageTime(
                                day = "$dateString/$month/$year",
                                hours = "$hoursString:$minutesString",
                                timestamp = message.time!!.timestamp
                            )
                            message.time = messageTime
                            message.id =  UUID.randomUUID().toString()
                            writeMessage(message)
                            runOnUiThread {
                                if (message.ruleMessage != true)
                                    messageAdapter.add(message)
                            }
                        }
                    }
                }
            })
        }

        val builder = AlertDialog.Builder(this)
        // builder.setTitle("Dialog Title")
        builder.setMessage(R.string.sessionFinishMessage)
        builder.setPositiveButton(R.string.yes) { _, _ ->
            val finishReq = SessionRequest(session.conversationId, session.channelId)
            mSocket.emit("finish_session", JSONObject(Gson().toJson(finishReq)))
        }
        builder.setNegativeButton(R.string.no) { _, _ ->
            // Do something when user clicks Cancel
        }
        val dialog = builder.create()

        val service = Service.getInstance()

        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val imageURL = "${service.url}/uploads/channels/${widgetSettings.data?.avatar}"
            var image: Bitmap? = null

            try {
                val `in` = java.net.URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                    chat_avatar.setImageBitmap(image)
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val lvm: ListView = findViewById<View>(R.id.listViewMessage) as ListView
        if (oldSessionId != null && session.conversationId == oldSessionId) {
            previousMessages = readMessage()
            runOnUiThread {
                previousMessages.forEach { prevMessage ->
                    if (prevMessage.ruleMessage != true)
                        messages.add(prevMessage)
                }
            }
        }

        writeUserInfo(user)

        lvm.adapter = messageAdapter
        setWidgetProperties(widgetSettings)
        listViewMessage.scrollY = listViewMessage.height

        val botUttered: (Any) -> Unit = { data: Any ->
            val args = (data as Array<*>).map { it.toString() }
            if (args[0] != null) {
                val gson = Gson()
                val message = gson.fromJson(args[0].toString(), Message::class.java)
                message.id =  UUID.randomUUID().toString()
                val hours = message.time?.timeObject?.hours
                val minutes = message.time?.timeObject?.minutes
                val date = message.time?.timeObject?.date
                val month = message.time?.timeObject?.month?.plus(1)
                val year = message.time?.timeObject?.year?.plus(1900)
                val hoursString =
                    if (hours!! < 10) "0$hours"
                    else "$hours"
                val minutesString =
                    if (minutes!! < 10) "0$minutes"
                    else "$minutes"
                val dateString =
                    if (date!! < 10) "0$date"
                    else "$date"

                val messageTime = MessageTime(
                    day = "$dateString/$month/$year",
                    hours = "$hoursString:$minutesString",
                    timestamp = message.time!!.timestamp
                )
                message.time = messageTime
                if (message.type != null) {
                    writeMessage(message)
                    runOnUiThread {
                        messageAdapter.add(message)
                    }
                }
            }
        }

        mSocket.on("bot_uttered", botUttered)

        send_button.visibility = View.GONE
        chatSender.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int,
            ) {}

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int,
            ) {
                if (chatSender.text.count() == 0 && send_button.visibility == View.VISIBLE) {
                    send_button.visibility = View.GONE
                } else if (chatSender.text.count() > 0 && send_button.visibility == View.GONE) {
                    send_button.visibility = View.VISIBLE
                }
            }
        })

        mSocket.on("session_finished") { args ->
            //if (widgetSettings.data?.showSurvey == true && messageAdapter.count > 0) {
            if (widgetSettings.data?.showSurvey == true) {
                runOnUiThread {
                    val message = showSurvey()
                    messageAdapter.add(message)
                }
            } else {
                clearMessages()
                clearSessionInfo()
                this.onBackPressed()
            }

        }

        close_session.setOnClickListener {
            dialog.show()
        }

        fun sendMessage(text: String?, rule: Boolean?) {
            if (session.channelId != null && session.conversationId != null &&
                session.userToken != null && text?.isNotEmpty()!!
            ) {
                val model = SendMessageModel(
                    session.channelId!!,
                    text,
                    session.conversationId!!,
                    session.userToken!!,
                    user
                )
                mSocket.emit("user_uttered", JSONObject(Gson().toJson(model)))

                val dayFormat = SimpleDateFormat("dd/M/yyyy")
                val hoursFormat = SimpleDateFormat("HH:mm")

                val currentDay = dayFormat.format(Date())

                val currentHours = hoursFormat.format(Date())
                val messageTime = MessageTime(
                    day = currentDay, hours = currentHours, timestamp = System.currentTimeMillis().toString())
                val newMessage = Message(
                    id =  UUID.randomUUID().toString(),
                    text = text,
                    fromCustomer = true,
                    type = "text",
                    time = messageTime,
                    ruleMessage = rule
                )
                writeMessage(newMessage)
                runOnUiThread {
                    if (newMessage.ruleMessage != true)
                        messageAdapter.add(newMessage)
                }
                chatSender.setText("")
            }
        }

        if (previousMessages.count() == 0 && widgetSettings.triggerRules?.get(0)?.enabled!!) {
            sendMessage(widgetSettings.triggerRules[0].text, true)
        }

        send_button.setOnClickListener {
            sendMessage(chatSender.text.toString(), false)
        }

        back_button.setOnClickListener {
            this.onBackPressed()
        }

        openMenu.setOnClickListener{
            val dialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.document_dialog, null)
            val btnClose = view.findViewById<ImageButton>(R.id.close_btn)
            btnClose.setOnClickListener {
                dialog.dismiss()
            }
            dialog.setCancelable(false)
            dialog.setContentView(view)
            dialog.show()

            view.findViewById<LinearLayout>(R.id.camera).setOnClickListener {
                dialog.dismiss()
            }
            view.findViewById<LinearLayout>(R.id.gallery).setOnClickListener {
                dialog.dismiss()
                val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                gallery.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivityForResult(gallery, pickImage)
            }
            view.findViewById<LinearLayout>(R.id.file).setOnClickListener {
                dialog.dismiss()
            }
            view.findViewById<LinearLayout>(R.id.location).setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == pickImage) {
            imageUri = data?.data
            //imageView.setImageURI(imageUri)
        }
    }

    override fun onBackPressed() {
        Exairon.isActive = false
        super.onBackPressed()
    }

    override fun onDestroy() {
        mSocket.off("bot_uttered")
        mSocket.on("bot_uttered") { args ->
            // TODO
        }

        super.onDestroy()
    }
}

