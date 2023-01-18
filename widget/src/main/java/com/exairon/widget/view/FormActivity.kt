package com.exairon.widget.view

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_form.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.exairon.widget.R
import com.exairon.widget.adaptor.FormAdapter
import com.exairon.widget.databinding.ActivityFormBinding
import com.exairon.widget.model.InitialUser
import com.exairon.widget.model.widgetSettings.FormElement
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.exairon.widget.Exairon
import com.exairon.widget.StateManager
import com.exairon.widget.model.Service
import com.exairon.widget.model.Session
import com.exairon.widget.model.User
import com.google.i18n.phonenumbers.PhoneNumberUtil
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_form.chat_avatar
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList

class FormActivity : AppCompatActivity() {

    lateinit var context: Context
    lateinit var binding: ActivityFormBinding

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
        val startSessionBtn = findViewById<Button>(R.id.start_session)

        topBorView.setBackgroundColor(Color.parseColor(colors?.headerColor))
        startSessionBtn.setBackgroundColor(Color.parseColor(colors?.headerColor))

        greetingTitleView.text = messages?.headerTitle
        greetingTitleView.setTextColor(Color.parseColor(colors?.headerFontColor))

        greetingMessageView.text = messages?.headerMessage
        greetingMessageView.setTextColor(Color.parseColor(colors?.headerFontColor))
    }

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

     private fun isValidEmail(target: CharSequence): Boolean {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private fun isValidPhone(target: CharSequence): Boolean {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val phoneNumber = phoneUtil.parse(target, null)
        return phoneUtil.isValidNumber(phoneNumber)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)

        binding = ActivityFormBinding.inflate(layoutInflater)
        context = this@FormActivity

        val config = resources.configuration
        val lang = Exairon.language // your language code
        val locale = Locale(lang)
        Locale.setDefault(locale)
        config.setLocale(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)

        val service = Service.getInstance()
        val widgetSettings = WidgetSettings.getInstance()
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

        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            this.onBackPressed()
        }

        this.setContentView(R.layout.activity_form)

        val user = InitialUser.getInstance()

        setWidgetProperties(widgetSettings)

        val formFields = ArrayList<FormElement>()
        val lvm: ListView = findViewById<View>(R.id.form_list) as ListView
        val fieldList: HashMap<String, View> = HashMap<String, View>()
        val formAdapter = FormAdapter(this, R.layout.form_field, formFields, fieldList)
        lvm.adapter = formAdapter

        val formFieldsData = widgetSettings.data?.formFields

        if (formFieldsData!!.showNameField) {
            val field = FormElement("Name", required = formFieldsData.nameFieldRequired, user.name)
            formAdapter.add(field)
        }
        if (formFieldsData.showSurnameField) {
            val field = FormElement("Surname", required = formFieldsData.surnameFieldRequired, user.surname)
            formAdapter.add(field)
        }
        if (formFieldsData.showEmailField) {
            val field = FormElement("E-Mail", required = formFieldsData.emailFieldRequired, user.email)
            formAdapter.add(field)
        }
        if (formFieldsData.showPhoneField) {
            val field = FormElement("Phone", required = formFieldsData.phoneFieldRequired, user.phone)
            formAdapter.add(field)
        }

        fun isValidForm(): Boolean {
            val nameView = findViewById<EditText>(R.id.name)
            val surnameView = findViewById<EditText>(R.id.surname)
            val emailView = findViewById<EditText>(R.id.email)
            val phoneView = findViewById<EditText>(R.id.phone)

            if (formFieldsData.nameFieldRequired && nameView.text.count() == 0)
                return false
            if (formFieldsData.surnameFieldRequired && surnameView.text.count() == 0)
                return false
            if (formFieldsData.showEmailField &&
                ((formFieldsData.emailFieldRequired || emailView.text.count() > 0) &&
                        !isValidEmail(emailView.text)))
                return false
            if (formFieldsData.showPhoneField &&
                ((formFieldsData.phoneFieldRequired || phoneView.text.count() > 0) &&
                        !isValidPhone(phoneView.text)))
                return false
            return true
        }

        fun showInvalidMessage() {
            val errorView = findViewById<TextView>(R.id.form_error)
            errorView.setTextColor(Color.parseColor("#ff0000"))
        }

        start_session.setOnClickListener {
            val value = isValidForm()
            if(!value) {
                showInvalidMessage()
                return@setOnClickListener
            }
            User.getInstance(
                name = findViewById<EditText>(R.id.name)?.text?.toString(),
                surname = findViewById<EditText>(R.id.surname)?.text?.toString(),
                email = findViewById<EditText>(R.id.email)?.text?.toString(),
                phone = findViewById<EditText>(R.id.phone)?.text?.toString())

            val session = StateManager.tempSession
            StateManager.tempSession = null
            if (session != null) {
                writeSessionInfo(session)
                StateManager.conversationId = session.conversationId
                StateManager.channelId = session.channelId
                StateManager.userToken = session.userToken
                val intent = Intent(this, ChatActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onBackPressed() {
        Exairon.isActive = false
        super.onBackPressed()
    }
}