package cu.todus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cu.todus.app.ui.navigation.NavGraph
import cu.todus.app.ui.theme.TodusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodusTheme {
                NavGraph()
            }
        }
    }
}
