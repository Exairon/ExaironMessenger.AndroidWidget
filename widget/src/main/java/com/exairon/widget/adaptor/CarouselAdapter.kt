package com.exairon.widget.adaptor

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.exairon.widget.R
import com.exairon.widget.model.*
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.socket.SocketHandler
import com.google.android.flexbox.FlexboxLayout
import com.google.gson.Gson
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import android.util.TypedValue
import com.exairon.widget.view.SomeDrawable

class CarouselAdapter(
    private val context: Activity,
    private val carouselDataList: ArrayList<Element>,
    private val widgetSettings: WidgetSettings?,
    private val session: Session,
    private val messageAdapter: MessageAdapter,
) :
    RecyclerView.Adapter<CarouselAdapter.CarouselItemViewHolder>() {

    class CarouselItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselItemViewHolder {
        val viewHolder = LayoutInflater.from(parent.context).inflate(R.layout.carousel_item, parent, false)
        return CarouselItemViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        val user = User.getInstance()
        val mSocket = SocketHandler.getSocket()

        val cardImageView = holder.itemView.findViewById<ImageView>(R.id.card_image)
        val cardTitleView = holder.itemView.findViewById<TextView>(R.id.card_title)
        val cardSubTitleView = holder.itemView.findViewById<TextView>(R.id.card_sub_title)

        //Image
        cardTitleView.text = carouselDataList[position].title
        cardSubTitleView.text = carouselDataList[position].subtitle
        executor.execute {
            val imageURL = carouselDataList[position].image_url
            var image: Bitmap? = null

            try {
                val `in` = java.net.URL(imageURL).openStream()
                image = BitmapFactory.decodeStream(`in`)
                handler.post {
                    cardImageView.setImageBitmap(image)
                }
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val lvm: FlexboxLayout = holder.itemView.findViewById<View>(R.id.card_buttonList) as FlexboxLayout

        //Buttons
        carouselDataList[position].buttons?.forEach { button ->
            val buttonView = Button(context)
            val drawable = SomeDrawable(Color.parseColor(widgetSettings?.data?.color?.buttonBackColor),
                Color.parseColor(widgetSettings?.data?.color?.buttonBackColor),
                Color.parseColor(widgetSettings?.data?.color?.buttonBackColor),
                3,
                Color.parseColor(widgetSettings?.data?.color?.buttonFontColor),
                200.0)

            buttonView.text = button.title
            buttonView.setPadding(30, 30, 30, 30)
            buttonView.minHeight = 0
            buttonView.minimumHeight = 0
            buttonView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11F)
            buttonView.background = drawable
            buttonView.setTextColor(Color.parseColor(widgetSettings?.data?.color?.buttonFontColor))
            buttonView.setOnClickListener {
                val type = button.type
                if (type.equals("postback")) {
                    val payload = button.payload
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
                            text = button.title,
                            fromCustomer = true,
                            type = "text",
                            time = messageTime)
                        messageAdapter.add(newMessage)
                    }
                } else {
                    if (button.url?.startsWith("https://") == true ||
                        button.url?.startsWith("http://") == true) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(button.url))
                        context.startActivity(browserIntent)
                    }
                }

            }
            lvm.addView(buttonView)
        }
    }

    override fun getItemCount(): Int {
        return carouselDataList.size
    }

}