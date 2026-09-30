package pl.edwin.budowlanka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.edwin.budowlanka.ui.BudTheme
import pl.edwin.budowlanka.ui.BudowlankaRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BudTheme {
                val vm: MainViewModel = viewModel()
                BudowlankaRoot(vm)
            }
        }
    }
}
