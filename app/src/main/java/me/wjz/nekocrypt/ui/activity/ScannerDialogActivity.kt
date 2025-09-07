package me.wjz.nekocrypt.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import me.wjz.nekocrypt.ui.dialog.ScannerDialog
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme

class ScannerDialogActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NekoCryptTheme {
                // 在这里显示我们的对话框
                // 当对话框请求关闭时，我们直接结束这个透明的 Activity
                ScannerDialog(onDismissRequest = { finish() })
            }
        }
    }
}