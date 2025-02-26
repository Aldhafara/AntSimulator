package com.aldhafara.ant_simulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AntSimulator(sizeDp = 500.dp)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    AntSimulator(sizeDp = 500.dp)
}