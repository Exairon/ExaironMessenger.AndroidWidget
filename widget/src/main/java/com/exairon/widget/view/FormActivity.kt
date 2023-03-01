package com.exairon.widget.view

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.exairon.widget.Exairon
import com.exairon.widget.R
import com.exairon.widget.StateManager
import com.exairon.widget.databinding.ActivityFormBinding
import com.exairon.widget.model.InitialUser
import com.exairon.widget.model.Service
import com.exairon.widget.model.Session
import com.exairon.widget.model.User
import com.exairon.widget.model.widgetSettings.WidgetSettings
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.hbb20.CountryCodePicker
import kotlinx.android.synthetic.main.activity_form.*
import kotlinx.android.synthetic.main.activity_form.chat_avatar
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors

class FormActivity : AppCompatActivity() {

    lateinit var context: Context
    lateinit var binding: ActivityFormBinding
    private lateinit var countryCodePicker :CountryCodePicker
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
        startSessionBtn.setTextColor(Color.parseColor(colors?.headerFontColor))

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
        var phone = "${countryCodePicker.selectedCountryCodeWithPlus}${target}"
        if (phone.isEmpty()) {
            return false
        }
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val phoneNumber = phoneUtil.parse(phone, null)
            return phoneUtil.isValidNumber(phoneNumber)
        } catch(e: Exception) {
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val widgetSettings = WidgetSettings.getInstance()
        when(widgetSettings.data?.font) {
            "Arial" -> setTheme(R.style.Arial)
            "Baloo-2" -> setTheme(R.style.Baloo2)
            "Calibri" -> setTheme(R.style.Calibri)
            "Cambria" -> setTheme(R.style.Cambria)
            "Comfortaa" -> setTheme(R.style.Comfortaa)
            "Georgia" -> setTheme(R.style.Georgia)
            "Helvetica" -> setTheme(R.style.Helvetica)
            "Inter" -> setTheme(R.style.Inter)
            "Kollektif" -> setTheme(R.style.Kollektif)
            "Lato" -> setTheme(R.style.Lato)
            "Lucida Sans" -> setTheme(R.style.LucidaSans)
            "Manrope" -> setTheme(R.style.Manrope)
            "Montserrat" -> setTheme(R.style.Montserrat)
            "Mulish" -> setTheme(R.style.Mulish)
            "Open Sans" -> setTheme(R.style.OpenSans)
            "Oswald" -> setTheme(R.style.Oswald)
            "Poppins" -> setTheme(R.style.Poppins)
            "Roboto" -> setTheme(R.style.Roboto)
            "San Francisco" -> setTheme(R.style.SanFrancisco)
            "Times New Roman" -> setTheme(R.style.TimesNewRoman)
            "Trebuchet MS" -> setTheme(R.style.TrebuchetMS)
            "Verdana" -> setTheme(R.style.Verdana)
            else -> setTheme(R.style.OpenSans)
        }

        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
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

        this.setContentView(R.layout.activity_form)

        val user = InitialUser.getInstance()

        setWidgetProperties(widgetSettings)

        val formFieldsData = widgetSettings.data?.formFields

        var phoneNumberUtil = PhoneNumberUtil.getInstance()
        countryCodePicker = findViewById(R.id.countyCodePicker)
        countryCodePicker.setCountryForNameCode("TR")

        fun getCountryIsoCode(number: String): String? {
            val validatedNumber = if (number.startsWith("+")) number else "+$number"

            val phoneNumber = try {
                phoneNumberUtil.parse(validatedNumber, null)
            } catch (e: NumberParseException) {
                println("error during parsing a number")
                null
            } ?: return null

            return phoneNumberUtil.getRegionCodeForCountryCode(phoneNumber.countryCode)
        }

        if (!formFieldsData!!.showNameField) {
            nameField.visibility = View.INVISIBLE
            val params: ViewGroup.LayoutParams = nameField.layoutParams
            params.height = 0
            nameField.layoutParams = params
        } else if (formFieldsData.nameFieldRequired) {
            nameText.text = "${nameText.text}*"
        }
        if (!formFieldsData.showSurnameField) {
            surnameField.visibility = View.INVISIBLE
            val params: ViewGroup.LayoutParams = surnameField.layoutParams
            params.height = 0
            surnameField.layoutParams = params
        } else if (formFieldsData.surnameFieldRequired) {
            surnameText.text = "${surnameText.text}*"
        }
        if (!formFieldsData.showEmailField) {
            emailField.visibility = View.INVISIBLE
            val params: ViewGroup.LayoutParams = emailField.layoutParams
            params.height = 0
            emailField.layoutParams = params
        } else if (formFieldsData.emailFieldRequired) {
            emailText.text = "${emailText.text}*"
        }
        if (!formFieldsData.showPhoneField) {
            phoneField.visibility = View.INVISIBLE
            val params: ViewGroup.LayoutParams = phoneField.layoutParams
            params.height = 0
            phoneField.layoutParams = params
        } else if (formFieldsData.phoneFieldRequired) {
            phoneText.text = "${phoneText.text}*"
        }

        if (Exairon.name != null && formFieldsData.showNameField) {
            name.setText(Exairon.name)
        }
        if (Exairon.surname != null && formFieldsData.showSurnameField) {
            surname.setText(Exairon.surname)
        }
        if (Exairon.email != null && formFieldsData.showEmailField) {
            email.setText(Exairon.email)
        }
        if (Exairon.phone != null && formFieldsData.showPhoneField) {
            val phoneUtil = PhoneNumberUtil.getInstance()
            try {
                val phoneNumber = phoneUtil.parse(Exairon.phone, null)
                val countryCode = getCountryIsoCode(Exairon.phone!!)
                if (countryCode != null) {
                    println(countryCode)
                    countryCodePicker.setCountryForNameCode(countryCode)
                    phone.setText(Exairon.phone!!.replace(phoneNumber.countryCode.toString(), "").replace("+", ""))
                }
            } catch (e: Exception) {
                phone.setText(Exairon.phone!!.replace("+", ""))
            }

        }

        fun isValidForm(): Boolean {
            val nameView = findViewById<EditText>(R.id.name)
            val surnameView = findViewById<EditText>(R.id.surname)
            val emailView = findViewById<EditText>(R.id.email)
            val phoneView = findViewById<EditText>(R.id.phone)

            if (formFieldsData != null) {
                if (formFieldsData.nameFieldRequired && nameView.text.isEmpty())
                    return false
            }
            if (formFieldsData != null) {
                if (formFieldsData.surnameFieldRequired && surnameView.text.isEmpty())
                    return false
            }
            if (formFieldsData != null) {
                if (formFieldsData.showEmailField &&
                    ((formFieldsData.emailFieldRequired || emailView.text.isNotEmpty()) &&
                            !isValidEmail(emailView.text)))
                    return false
            }
            if (formFieldsData != null) {
                if (formFieldsData.showPhoneField &&
                    ((formFieldsData.phoneFieldRequired || phoneView.text.isNotEmpty()) &&
                            !isValidPhone(phoneView.text)))
                    return false
            }
            return true
        }

        fun showInvalidMessage() {
            val errorView = findViewById<TextView>(R.id.form_error)
            errorView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
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
                phone = "${countryCodePicker.selectedCountryCodeWithPlus}${findViewById<EditText>(R.id.phone)?.text?.toString()}",
                user_unique_id = Exairon.user_unique_id
                )

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


        back_button_form.setOnClickListener {
            this.onBackPressed()
        }
    }

    override fun onBackPressed() {
        Exairon.isActive = false
        super.onBackPressed()
    }
}