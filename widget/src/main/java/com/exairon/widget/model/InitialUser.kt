package com.exairon.widget.model

data class InitialUser(
    val email: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val surname: String? = null
) {
    companion object {
        @Volatile
        @JvmStatic
        private var INSTANCE: User? = null

        @JvmStatic
        @JvmOverloads
        fun getInstance(email: String? = "", name : String? = "", phone : String? = "", surname: String? = ""): User = INSTANCE ?: synchronized(this) {
            INSTANCE ?: User(email, name, phone, surname).also { INSTANCE = it }
        }
    }
}