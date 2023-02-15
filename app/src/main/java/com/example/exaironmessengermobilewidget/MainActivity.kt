package com.example.exaironmessengermobilewidget

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.exairon.widget.Exairon
import com.example.exaironmessengermobilewidget.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        //Exairon.src = "http://10.0.96.176:3001"
        //Exairon.channelId = "63a44dfb82dcf01184609988"
        Exairon.src = "https://cafd-212-154-84-123.eu.ngrok.io"
        Exairon.channelId = "63b2bb73f8ef51000a570a9e"
        //Exairon.src = "https://bisu.services.exairon.com"
        //Exairon.channelId = "63beb38a95be6a000ada12b0"
        Exairon.language = "tr"
        Exairon.name = "John"
        Exairon.surname = "Doe"
        Exairon.user_unique_id = "unique"
        Exairon.phone = "+905555555555"
        Exairon.email = "fatih_kesici@gmail.com"

        binding.fab.setOnClickListener { view ->
            Exairon.startChatActivity(this)
        }
    }

    override fun onResume() {
        Exairon.init(this)
        super.onResume()
    }
}