package com.exairon.widget.adaptor

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.exairon.widget.R
import com.exairon.widget.model.widgetSettings.FormElement
import org.jetbrains.anko.sdk27.coroutines.onFocusChange
import java.util.HashMap

class FormAdapter(
    private val context: Activity,
    private var res: Int,
    private val arrayList: ArrayList<FormElement>,
    private val fieldList: HashMap<String, View>,
    private val showSoftKeyboard: (View) -> Unit
) :
    ArrayAdapter<FormElement?>(context, res, arrayList as List<FormElement?>) {

    @SuppressLint("ViewHolder")
    @RequiresApi(Build.VERSION_CODES.N)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (fieldList.containsKey(getItem(position)?.field)) {
            return fieldList[getItem(position)?.field]!!
        }
        val inflater = LayoutInflater.from(context)

        val convertView = inflater.inflate(res, parent, false)

        val formFieldTitle = convertView?.findViewById<TextView>(R.id.form_field_title)
        val formFieldInput = convertView?.findViewById<EditText>(R.id.form_field_input)

        var title = getItem(position)!!.field
        if (getItem(position)!!.required) title += "*"
        formFieldTitle?.text = title
        formFieldInput?.hint = "Please enter your " + getItem(position)!!.field
        formFieldInput?.setText(getItem(position)!!.value)

        formFieldInput?.onFocusChange { v, hasFocus ->
            if (hasFocus) {
                showSoftKeyboard(v)
            }
        }

        when(getItem(position)!!.field) {
            "Name" -> formFieldInput?.id = R.id.name
            "Surname" -> formFieldInput?.id = R.id.surname
            "E-Mail" -> formFieldInput?.id = R.id.email
            "Phone" -> formFieldInput?.id = R.id.phone
        }

        fieldList[getItem(position)?.field!!] = convertView
        return convertView
    }
}