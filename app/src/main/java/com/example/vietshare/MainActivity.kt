package com.example.vietshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.vietshare.ui.AppNavigation
import com.example.vietshare.ui.theme.VietShareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VietShareTheme {
                AppNavigation()
            }
        }
    }
}
