package com.kichikan.ak1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kichikan.ak1.ui.dashboard.DashboardScreen
import com.kichikan.ak1.ui.theme.AK1Theme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            AK1Theme {

                DashboardScreen()

            }
        }
    }
}
