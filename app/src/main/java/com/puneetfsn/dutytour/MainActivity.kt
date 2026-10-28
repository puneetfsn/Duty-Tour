package com.puneetfsn.dutytour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.puneetfsn.dutytour.ui.DutyTourNavHost
import com.puneetfsn.dutytour.ui.theme.DutyTourTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DutyTourTheme {
                DutyTourNavHost()
            }
        }
    }
}
