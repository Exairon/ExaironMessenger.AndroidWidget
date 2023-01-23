package com.exairon.widget.view

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.exairon.widget.Exairon
import com.exairon.widget.R
import com.exairon.widget.StateManager
import com.exairon.widget.adaptor.MessageAdapter
import com.exairon.widget.databinding.ActivityChatBinding
import com.exairon.widget.model.*
import com.exairon.widget.model.Message
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.socket.SocketHandler
import com.exairon.widget.viewmodel.ChatActivityViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.document_dialog.*
import kotlinx.android.synthetic.main.text_message_bot.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xmlpull.v1.XmlPullParserException
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import javax.xml.parsers.DocumentBuilderFactory

class ChatActivity : AppCompatActivity() {

    lateinit var context: Context
    lateinit var binding: ActivityChatBinding
    private val STORAGE_PERMISSION_CODE: Int = 1000
    var itemDownload : Long = 0
    private lateinit var fileSrc: String
    lateinit var fileName: String
    private val mSocket = SocketHandler.getSocket()
    private var imageUri: Uri? = null
    private var state = false

    private val GALLERY_PERMISSION_CODE = 100
    private val GALLERY_REQUEST_CODE = 101

    private val CAMERA_PERMISSION_CODE = 200
    private val CAMERA_REQUEST_CODE = 201

    private val DOCUMENT_PERMISSION_CODE = 300
    private val DOCUMENT_REQUEST_CODE = 301
    var mimeTypes = arrayOf(
        "image/*",
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    private lateinit var messageAdapter: MessageAdapter

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
        val oldMessages = text.replace("<root>", "").replace("</root>", "")
        var xmlString = "<root>" +
                oldMessages +
                "<message>" +
                "<time>" +
                "<day>${message.time?.day ?: ""}</day>" +
                "<hours>${message.time?.hours ?: ""}</hours>" +
                "<timestamp>${message.time?.timestamp ?: ""}</timestamp>" +
                "</time>" +
                "<fromCustomer>${message.fromCustomer?.toString() ?: ""}</fromCustomer>" +
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

        }
        xmlString += "</message></root>"
        val fileLastOutputStream = FileOutputStream(fileLast)
        val time = if (message.fromCustomer == true) {
            message.time?.timestamp
        } else {
            message.time?.timestamp
        }
        val lastString = "<root><lastMessageTime>${time ?: ""}</lastMessageTime></root>"

        fileOutputStream.write(xmlString.toByteArray())
        fileLastOutputStream.write(lastString.toByteArray())

        fileOutputStream.close()
        fileLastOutputStream.close()
    }

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
                            message = Message(text = title, fromCustomer = fromCustomer, type = messageType, quick_replies = buttonList, time = messageTime, ruleMessage = rule)
                        }
                        "video" -> {
                            val videoType = getNodeValue("videoType", element)
                            val src = getNodeValue("src", element).replace("&lt;", "<")
                            val payload = Payload(videoType = videoType, src = src)
                            val attachment = Attachment(payload = payload)
                            message = Message(type = messageType, attachment = attachment, fromCustomer = fromCustomer, time = messageTime, ruleMessage = rule)
                        }
                        "audio" -> {
                            val src = getNodeValue("src", element).replace("&lt;", "<")
                            val payload = Payload(src = src)
                            val attachment = Attachment(payload = payload)
                            val data = CustomData(attachment = attachment)
                            val custom = Custom(data = data)
                            message = Message(type = messageType, custom = custom, time = messageTime, fromCustomer = fromCustomer, ruleMessage = rule)
                        }
                        "document" -> {
                            val originalName = getNodeValue("originalname", element).replace("&lt;", "<")
                            val originalSrc = getNodeValue("src", element).replace("&lt;", "<")
                            val payload = Payload(src = originalSrc, originalname = originalName)
                            val attachment = Attachment(payload = payload)
                            val data = CustomData(attachment = attachment)
                            val custom = Custom(data = data)
                            message = Message(type = messageType, custom = custom, time = messageTime, fromCustomer = fromCustomer, ruleMessage = rule)
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
                            message = Message(attachment = attachment, type = messageType, time = messageTime, ruleMessage = rule, fromCustomer = fromCustomer)
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
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)
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
        clearMessages()
        clearSessionInfo()
        if (close_session.visibility == View.VISIBLE) {
            close_session.visibility = View.GONE
        }
        if (chatSendArea.visibility == View.VISIBLE){
            chatSendArea.visibility = View.GONE
        }
        return message
    }

    @SuppressLint("InflateParams")
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
        messageAdapter = MessageAdapter(
            this,
            R.layout.text_message_bot,
            messages,
            messageList,
            widgetSettings
        )

        val lastMessageTime = getLastMessageTime()

        if (lastMessageTime != null && lastMessageTime.isNotEmpty() &&
            session.conversationId != null && session.conversationId!!.isNotEmpty()
        ) {
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
                        messageAdapter.add(prevMessage)
                }
            }
        }

        writeUserInfo(user)

        lvm.adapter = messageAdapter
        setWidgetProperties(widgetSettings)
        listViewMessage.scrollY = listViewMessage.height

        val botUttered: (Any) -> Unit = { data: Any ->
            val args = (data as Array<*>).map { it.toString() }
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
                if (chatSender.text.isEmpty() && send_button.visibility == View.VISIBLE) {
                    send_button.visibility = View.GONE
                } else if (chatSender.text.isNotEmpty() && send_button.visibility == View.GONE) {
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

        @SuppressLint("SimpleDateFormat")
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

        if (previousMessages.isEmpty() && widgetSettings.triggerRules?.size!! > 0 && widgetSettings.triggerRules[0].enabled!!) {
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
                requestCameraPermission()
            }
            view.findViewById<LinearLayout>(R.id.gallery).setOnClickListener {
                dialog.dismiss()
                pickPhoto()
            }
            view.findViewById<LinearLayout>(R.id.file).setOnClickListener {
                dialog.dismiss()
                pickDocument()
            }
            /*view.findViewById<LinearLayout>(R.id.location).setOnClickListener {
                dialog.dismiss()
            }*/
        }
    }
    private fun pickPhoto(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                GALLERY_PERMISSION_CODE)
        } else {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent,GALLERY_REQUEST_CODE)
        }
    }

    private fun pickDocument(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                DOCUMENT_PERMISSION_CODE)
        } else {
            openDocumentInterface()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when(requestCode) {
                GALLERY_PERMISSION_CODE -> {
                    val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent,GALLERY_REQUEST_CODE)
                }
                CAMERA_PERMISSION_CODE -> {
                    openCameraInterface()
                }
                DOCUMENT_PERMISSION_CODE -> {
                    openDocumentInterface()
                }
            }
        } else {
            // Permission Error
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("Range")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val chatActivityViewModel = ViewModelProvider(this)[ChatActivityViewModel::class.java]
        val thisContext = this
        state = false
        if (resultCode == Activity.RESULT_OK) {
            val uri = if (requestCode == CAMERA_REQUEST_CODE) {
                imageUri
            } else {
                data?.data
            }
            val cR = context.contentResolver
            val iStream = contentResolver.openInputStream(uri!!)

            val inputData: ByteArray? = iStream?.buffered()?.use { it.readBytes() }
            iStream?.close()

            val mime = cR.getType(uri)!!
            val filename = getFileName(uri)

            contentResolver?.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val requestBody =
                        inputData!!.toRequestBody(mime.toMediaTypeOrNull(), 0, inputData.size)
                    val filePart = MultipartBody.Part.createFormData(
                        "uploadedFileForChat",
                        filename,
                        requestBody
                    )
                    val session = getSessionInfo()
                    session.conversationId?.let { it1 ->
                        chatActivityViewModel.uploadFileForChat(filePart,
                            it1
                        )
                    }!!.observe(thisContext, Observer { uploadResponse ->
                        if (!state && uploadResponse.status == "success") {
                            state = true
                            val uploadData = uploadResponse.data
                            val user = User.getInstance()

                            val fileModel = FileMessageModel(
                                document = uploadData?.url,
                                mimeType = uploadData?.mimeType,
                                originalname = uploadData?.originalname
                            )
                            val model = SendFileMessageModel(
                                session.channelId!!,
                                fileModel,
                                session.conversationId!!,
                                session.userToken!!,
                                user
                            )
                            mSocket.emit("user_uttered", JSONObject(Gson().toJson(model)))

                            val dayFormat = SimpleDateFormat("dd/M/yyyy")
                            val hoursFormat = SimpleDateFormat("HH:mm")

                            val currentDay = dayFormat.format(Date())

                            var attachment: Attachment? = null
                            var custom: Custom? = null

                            val messageType = if (requestCode != DOCUMENT_REQUEST_CODE) {
                                val payload = Payload(src = uploadData?.url)
                                attachment = Attachment(payload = payload)
                                "image"
                            } else {
                                val payload = Payload(src = uploadData?.url, originalname = uploadData?.originalname)
                                val documentAttachment = Attachment(payload = payload)
                                val data = CustomData(attachment = documentAttachment)
                                custom = Custom(data = data)
                                "document"
                            }

                            val currentHours = hoursFormat.format(Date())
                            val messageTime = MessageTime(
                                day = currentDay, hours = currentHours, timestamp = System.currentTimeMillis().toString())
                            val newMessage = Message(
                                id =  UUID.randomUUID().toString(),
                                attachment = attachment,
                                custom = custom,
                                fromCustomer = true,
                                type = messageType,
                                time = messageTime,
                            )
                            writeMessage(newMessage)
                            runOnUiThread {
                                messageAdapter.add(newMessage)
                            }
                        }
                    })
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    private fun requestCameraPermission() {
        // If system os is Marshmallow or Above, we need to request runtime permission
        val cameraPermissionNotGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_DENIED
        if (cameraPermissionNotGranted){
            val permission = arrayOf(Manifest.permission.CAMERA)
            requestPermissions(permission, CAMERA_PERMISSION_CODE)
        }
        else{
            // Permission already granted
            openCameraInterface()
        }
    }

    @SuppressLint("Range")
    fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    private fun openDocumentInterface() {
        val pdfIntent = Intent(Intent.ACTION_GET_CONTENT)
        var mimeTypesStr = ""
        for (mimeType in mimeTypes) {
            mimeTypesStr += "$mimeType|"
        }
        pdfIntent.type = "application/pdf|application/vnd.ms-powerpoint|application/vnd.ms-excel|image/*"
        pdfIntent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(pdfIntent, DOCUMENT_REQUEST_CODE)
    }
    private fun openCameraInterface() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, R.string.take_picture)
        values.put(MediaStore.Images.Media.DESCRIPTION, R.string.take_picture_description)
        imageUri = this.contentResolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        // Create camera intent
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        // Launch intent
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
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

