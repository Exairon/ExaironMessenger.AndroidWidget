package com.exairon.widget.model

data class User(
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val surname: String? = null,
    val user_unique_id: String? = null
) {
    companion object {
        @Volatile
        @JvmStatic
        private var INSTANCE: User? = null

        @JvmStatic
        @JvmOverloads
        fun getInstance(email: String? = "", name : String? = "", phone : String? = "", surname: String? = "", user_unique_id: String? = ""):
                User = INSTANCE ?: synchronized(this) {
            INSTANCE ?: User(email, name, phone, surname, user_unique_id).also { INSTANCE = it }
        }
    }
}