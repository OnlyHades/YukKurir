package id.yukkurir.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import id.yukkurir.app.ui.YukApp
import id.yukkurir.app.ui.theme.YukKurirTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YukKurirTheme {
                YukApp()
            }
        }
    }
}
