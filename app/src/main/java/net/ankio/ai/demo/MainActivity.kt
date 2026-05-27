package net.ankio.ai.demo

import androidx.compose.runtime.Composable
import net.ankio.theme.BaseComposeActivity

class MainActivity : BaseComposeActivity() {

    @Composable
    override fun Content() {
        DemoMainScreen()
    }
}
