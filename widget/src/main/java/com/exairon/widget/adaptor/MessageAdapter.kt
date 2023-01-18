package com.exairon.widget.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.ct7ct7ct7.androidvimeoplayer.view.VimeoPlayerView
import com.exairon.widget.Exairon
import com.exairon.widget.R
import com.exairon.widget.StateManager
import com.exairon.widget.model.*
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.socket.SocketHandler
import com.exairon.widget.view.ChatActivity
import com.google.android.flexbox.FlexboxLayout
import com.google.gson.Gson
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.abs
import com.exairon.widget.view.SomeDrawable

class MessageAdapter(
    private val context: Activity,
    private var res: Int,
    private val arrayList: ArrayList<Message>,
    private val messageList: HashMap<String, View>,
    private val widgetSettings: WidgetSettings?,
) :
    ArrayAdapter<Message?>(context, res, arrayList as List<Message?>) {

    private fun extractVideoId(ytUrl: String?): String? {
        var videoId: String? = null
        val regex =
            "^((?:https?:)?//)?((?:www|m)\\.)?((?:youtube\\.com|youtu.be|youtube-nocookie.com))(/(?:[\\w\\-]+\\?v=|feature=|watch\\?|e/|embed/|v/)?)([\\w\\-]+)(\\S+)?\$"
        val pattern: Pattern = Pattern.compile(
            regex ,
            Pattern.CASE_INSENSITIVE
        )
        val matcher: Matcher = pattern.matcher(ytUrl)
        if (matcher.matches()) {
            videoId = matcher.group(5)
        }
        return videoId
    }

    private fun clearMessages() {
        val xmlString = "<root></root>"
        val file = File(context.getExternalFilesDir(null), "messages.xml")
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

    private fun setWidgetProperties(context: Activity, parent: ViewGroup) {
        val widgetSettings = WidgetSettings.getInstance()
        val inflater = LayoutInflater.from(context)
        val convertView = inflater.inflate(R.layout.activity_chat, parent, false)
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
        val topBorView = convertView.findViewById<ConstraintLayout>(R.id.top_bar)
        val greetingTitleView = convertView.findViewById<TextView>(R.id.greeting_tite)
        val greetingMessageView = convertView.findViewById<TextView>(R.id.greeting_message)
        val editTextView = convertView.findViewById<EditText>(R.id.chatSender)

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

    @SuppressLint("ResourceType")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (messageList.containsKey(getItem(position)?.id)) {
            return messageList[getItem(position)?.id]!!
        }
        val inflater = LayoutInflater.from(context)
        val messageType = getItem(position)?.type
        var convertView = inflater.inflate(R.layout.text_message_customer, parent, false)
        val executor = Executors.newSingleThreadExecutor()
        val handlerImage = Handler(Looper.getMainLooper())
        val session = getSessionInfo()
        val user = User.getInstance()
        val mSocket = SocketHandler.getSocket()
        when (messageType) {
            "text" -> {
                val textColor: Int
                convertView = if (getItem(position)!!.fromCustomer == true) {
                    textColor = Color.parseColor(widgetSettings?.data?.color?.userMessageFontColor)
                    inflater.inflate(R.layout.text_message_customer, parent, false)
                } else {
                    textColor = Color.parseColor(widgetSettings?.data?.color?.botMessageFontColor)
                    inflater.inflate(R.layout.text_message_bot, parent, false)
                }

                val text = getItem(position)!!.text?.replace("&lt;", "<")
                    ?.replace("<p>", "")?.replace("</p>", "")

                val textMessageView = convertView.findViewById<TextView>(R.id.message)
                textMessageView.setTextColor(textColor)
                textMessageView.text = Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)

                convertView.setOnClickListener {
                    if (getItem(position)!!.text?.contains("href", ignoreCase = true) == true) {
                        try {
                            val parsedValue = Jsoup.parse(text)
                            val aTags = parsedValue.select("a")
                            val aTag = aTags[0]
                            val value = aTag.attr("href")
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(value))
                            context.startActivity(browserIntent)
                        } catch (e: java.lang.Exception) {
                            //
                        }

                    }
                }
            }
            "image" -> {
                convertView = inflater.inflate(R.layout.image_message, parent, false)
                val imageMessageView = convertView.findViewById<ImageView>(R.id.image_message)
                executor.execute {
                    val imageURL = getItem(position)?.attachment?.payload?.src
                    var image: Bitmap? = null

                    try {
                        val `in` = URL(imageURL).openStream()
                        image = BitmapFactory.decodeStream(`in`)
                        handlerImage.post {
                            imageMessageView.setImageBitmap(image)
                        }
                    }
                    catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            "button" -> {
                convertView = inflater.inflate(R.layout.button_message_list, parent, false)
                val lvm: FlexboxLayout = convertView.findViewById<View>(R.id.buttonList) as FlexboxLayout
                getItem(position)?.quick_replies?.forEach { quickReply ->
                    val button = Button(context)
                    button.text = quickReply.title
                    button.setTextColor(Color.parseColor(widgetSettings?.data?.color?.buttonFontColor))
                    val drawable = SomeDrawable(Color.parseColor(widgetSettings?.data?.color?.buttonBackColor),
                        Color.parseColor(widgetSettings?.data?.color?.buttonBackColor),
                        Color.parseColor(widgetSettings?.data?.color?.buttonBackColor),
                        3,
                        Color.parseColor(widgetSettings?.data?.color?.buttonFontColor),
                        200.0)
                    button.background = drawable
                    button.setPadding(30, 30, 30, 30)
                    button.minHeight = 0
                    button.minimumHeight = 0
                    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11F)
                    button.setOnClickListener {
                        val type = quickReply.type
                        if (type.equals("postback")) {
                            val payload = quickReply.payload
                            if (payload != null && session.channelId != null && session.conversationId != null
                                && session.userToken != null) {
                                val model = SendMessageModel(
                                    session.channelId!!,
                                    payload,
                                    session.conversationId!!,
                                    session.userToken!!,
                                    user
                                )
                                mSocket.emit("user_uttered", JSONObject(Gson().toJson(model)) )
                                val dayFormat = SimpleDateFormat("dd/M/yyyy")
                                val hoursFormat = SimpleDateFormat("HH:mm")
                                val currentDay = dayFormat.format(Date())
                                val currentHours = hoursFormat.format(Date())
                                val messageTime = MessageTime(
                                    day = currentDay, hours = currentHours, timestamp = System.currentTimeMillis().toString())
                                val newMessage = Message(
                                    id =  UUID.randomUUID().toString(),
                                    text = quickReply.title,
                                    fromCustomer = true,
                                    type = "text",
                                    time = messageTime)
                                this.add(newMessage)
                            }
                        } else {
                            if (quickReply.url?.startsWith("https://") == true ||
                                quickReply.url?.startsWith("http://") == true) {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(quickReply.url))
                                context.startActivity(browserIntent)
                            }
                        }

                    }
                    lvm.addView(button)
                }

                val buttonTitle = getItem(position)!!.text
                val buttonTitleView = convertView.findViewById<TextView>(R.id.button_title)
                buttonTitleView.setTextColor(Color.parseColor(widgetSettings?.data?.color?.botMessageFontColor))
                buttonTitleView.text = buttonTitle
            }
            "video" -> {
                val videoType = getItem(position)?.attachment?.payload?.videoType
                val videoUrl = getItem(position)!!.attachment?.payload?.src
                when (videoType) {
                    "local" -> {
                        convertView = inflater.inflate(R.layout.video_message_local, parent, false)
                        val videoView = convertView.findViewById<VideoView>(R.id.videoView)
                        val uri: Uri = Uri.parse(videoUrl)
                        videoView.setVideoURI(uri)
                        val mediaController = MediaController(context)
                        mediaController.setAnchorView(videoView)
                        mediaController.setMediaPlayer(videoView)
                        videoView.setMediaController(mediaController)
                    }
                    "youtube" -> {
                        convertView = inflater.inflate(R.layout.video_message_youtube, parent, false)
                        val videoID = extractVideoId(videoUrl)
                        val youtubePlayerView = convertView.findViewById<YouTubePlayerView>(R.id.youTubePlayerView)
                        youtubePlayerView.enterFullScreen()
                        youtubePlayerView.toggleFullScreen()
                        youtubePlayerView.getPlayerUiController()
                        youtubePlayerView.enterFullScreen()
                        youtubePlayerView.toggleFullScreen()
                        youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                            override fun onReady(youTubePlayer: YouTubePlayer) {
                                // loading the selected video
                                // into the YouTube Player
                                if (videoID != null) {
                                    youTubePlayer.loadVideo(videoID, 0f)
                                }
                            }
                            /* override fun onStateChange(
                                        youTubePlayer: YouTubePlayer,
                                        state: PlayerConstants.PlayerState
                                    ) {
                                        // this method is called if video has ended,
                                        super.onStateChange(youTubePlayer, state)
                                    } */
                        })
                    }
                    "vimeo" -> {
                        convertView = inflater.inflate(R.layout.video_message_vimeo, parent, false)
                        val vimeoPlayerView = convertView.findViewById<VimeoPlayerView>(R.id.vimeoPlayerView)
                        val videoId = videoUrl?.substring(videoUrl.lastIndexOf("/") + 1, videoUrl.length);
                        videoId?.toInt()?.let { vimeoPlayerView.initialize(true, it) }
                    }
                }
            }
            "audio" -> {
                convertView = inflater.inflate(R.layout.audio_message, parent, false)
                var totalTime = 0
                var mediaPlayer = MediaPlayer()
                val playBtn = convertView.findViewById<ImageButton>(R.id.playButton)
                val positionBar = convertView.findViewById<SeekBar>(R.id.position_bar)
                val elapsedTimeView = convertView.findViewById<TextView>(R.id.elapsed_time)
                val remainingTimeView = convertView.findViewById<TextView>(R.id.remaining_time)

                fun createTimeLabel(time: Int): String {
                    var timeLabel = ""
                    val min = time / 1000 / 60
                    val sec = time / 1000 % 60

                    timeLabel = "$min:"
                    if (sec < 10) timeLabel += "0"
                    timeLabel += sec

                    return timeLabel
                }

                @SuppressLint("HandlerLeak")
                val handler = object: Handler() {
                    override fun handleMessage(msg: android.os.Message) {
                        val currentPosition: Int = msg.what

                        positionBar.progress = currentPosition

                        val elapsedTime: String = createTimeLabel(currentPosition)
                        elapsedTimeView.text = elapsedTime

                        val remainingTime = createTimeLabel(totalTime - currentPosition)
                        remainingTimeView.text = "-$remainingTime"
                    }
                }

                try {
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(getItem(position)?.custom?.data?.attachment?.payload?.src)
                        setVolume(0.5f, 0.5f)
                        setOnCompletionListener {
                            playBtn.setBackgroundResource(R.drawable.play)
                        }
                        prepare()
                    }
                    totalTime = mediaPlayer.duration
                    positionBar.max = totalTime
                    positionBar.setOnSeekBarChangeListener(
                        object: SeekBar.OnSeekBarChangeListener {
                            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                                if (fromUser) {
                                    mediaPlayer.seekTo(progress)
                                }
                            }
                            override fun onStartTrackingTouch(p0: SeekBar?) {
                            }
                            override fun onStopTrackingTouch(p0: SeekBar?) {
                            }
                        }
                    )

                    Thread(Runnable {
                        while (mediaPlayer != null) {
                            try {
                                var msg = android.os.Message()
                                msg.what = mediaPlayer.currentPosition
                                handler.sendMessage(msg)
                                Thread.sleep(1000)
                            } catch (e: InterruptedException) {}
                        }
                    }).start()

                } catch (e: IOException) {
                    e.printStackTrace()
                }
                playBtn.setOnClickListener {
                    if (mediaPlayer.isPlaying) {
                        mediaPlayer.pause()
                        playBtn.setBackgroundResource(R.drawable.play)
                    } else {
                        mediaPlayer.start()
                        playBtn.setBackgroundResource(R.drawable.pause)
                    }
                }
            }
            "document" -> {
                convertView = inflater.inflate(R.layout.document_message, parent, false)
                val originalName = getItem(position)?.custom?.data?.attachment?.payload?.originalname
                val originalSrc = getItem(position)?.custom?.data?.attachment?.payload?.src

                val documentBtn = convertView.findViewById<Button>(R.id.documentName)
                documentBtn.text = originalName

                documentBtn.setOnClickListener {
                    if (context is ChatActivity) {
                        if (originalName != null && originalSrc != null) {
                            (context as ChatActivity).checkPermission(originalSrc,originalName)
                        }
                    }
                }
            }
            "carousel" -> {
                convertView = inflater.inflate(R.layout.carousel_message, parent, false)
                val viewPager = convertView.findViewById<ViewPager2>(R.id.view_pager)

                viewPager.apply {
                    clipChildren = false  // No clipping the left and right items
                    clipToPadding = false  // Show the viewpager in full width without clipping the padding
                    offscreenPageLimit = 3  // Render the left and right items
                    (getChildAt(0) as RecyclerView).overScrollMode =
                        RecyclerView.OVER_SCROLL_NEVER // Remove the scroll effect
                }

                val cards = ArrayList<Element>()
                getItem(position)?.attachment?.payload?.elements?.map {
                        element ->
                    cards.add(element)
                }
                viewPager.adapter = CarouselAdapter(context, cards, widgetSettings, session, this)

                val compositePageTransformer = CompositePageTransformer()
                compositePageTransformer.addTransformer(MarginPageTransformer((40 * Resources.getSystem().displayMetrics.density).toInt()))
                compositePageTransformer.addTransformer { page, position ->
                    val r = 1 - abs(position)
                    page.scaleY = (0.80f + r * 0.20f)
                }
                viewPager.setPageTransformer(compositePageTransformer)
            }
            "survey" -> {
                var value = "3"
                convertView = inflater.inflate(R.layout.survey, parent, false)
                convertView.findViewById<Button>(R.id.submitBtn).setOnClickListener {
                    val surveyResult = SurveyResult(
                        comment = convertView.findViewById<EditText>(R.id.surveyTextArea).text.toString(),
                        value = value
                    )
                    val surveyRequest = SurveyRequest(
                        channelId = session.channelId, session_id = session.conversationId, surveyResult = surveyResult)
                    mSocket.emit("send_survey_result", JSONObject(Gson().toJson(surveyRequest)))
                    val finishReq = SessionRequest(session.conversationId, session.channelId)
                    mSocket.emit("finish_session", JSONObject(Gson().toJson(finishReq)))
                    clearMessages()
                    clearSessionInfo()
                    context.onBackPressed()
                }

                val imageButtons = ArrayList<ImageButton>()
                imageButtons.add(convertView.findViewById(R.id.survey1))
                imageButtons.add(convertView.findViewById(R.id.survey2))
                imageButtons.add(convertView.findViewById(R.id.survey3))
                imageButtons.add(convertView.findViewById(R.id.survey4))
                imageButtons.add(convertView.findViewById(R.id.survey5))

                fun clear() {
                    imageButtons.forEach { button ->
                        var drawable = R.drawable.survey1_passive
                        when(button.id) {
                            R.id.survey1 -> drawable = R.drawable.survey1_passive
                            R.id.survey2 -> drawable = R.drawable.survey2_passive
                            R.id.survey3 -> drawable = R.drawable.survey3_passive
                            R.id.survey4 -> drawable = R.drawable.survey4_passive
                            R.id.survey5 -> drawable = R.drawable.survey5_passive
                        }
                        button.setBackgroundDrawable(ContextCompat.getDrawable(context, drawable))
                    }
                }

                imageButtons.forEach { button ->
                    button.setOnClickListener {
                        clear()
                        var drawable = R.drawable.survey1
                        when(button.id) {
                            R.id.survey1 -> {
                                value = "1"
                                drawable = R.drawable.survey1
                            }
                            R.id.survey2 -> {
                                value = "2"
                                drawable = R.drawable.survey2
                            }
                            R.id.survey3 -> {
                                value = "3"
                                drawable = R.drawable.survey3
                            }
                            R.id.survey4 -> {
                                value = "4"
                                drawable = R.drawable.survey4
                            }
                            R.id.survey5 -> {
                                value = "5"
                                drawable = R.drawable.survey5
                            }
                        }
                        button.setBackgroundDrawable(ContextCompat.getDrawable(context, drawable))
                    }
                }

            }
        }

        val messageTimeView = convertView.findViewById<TextView>(R.id.message_time)
        val hours = getItem(position)?.time?.hours

        val dayFormat = SimpleDateFormat("dd/M/yyyy")
        val currentDay = dayFormat.format(Date())
        if (getItem(position)?.time?.day != null) {
            if (!currentDay.equals(getItem(position)?.time?.day)) {
                messageTimeView.text = "${getItem(position)?.time?.day} $hours"
            } else {
                messageTimeView.text = hours
            }
        }

        messageTimeView?.gravity = if (getItem(position)!!.fromCustomer == true) {
            Gravity.END
        } else {
            Gravity.START
        }

        setWidgetProperties(context, parent)

        messageList[getItem(position)?.id!!] = convertView
        return messageList[getItem(position)?.id]!!
    }
}