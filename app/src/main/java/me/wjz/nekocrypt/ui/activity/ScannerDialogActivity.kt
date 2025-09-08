package me.wjz.nekocrypt.ui.activity

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import me.wjz.nekocrypt.Constant.SCAN_RESULT
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.dialog.ScanResult
import me.wjz.nekocrypt.ui.dialog.ScannerDialog
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme

class ScannerDialogActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✨ 核心魔法：从送来的“快递盒”(Intent)中，把名叫"scan_result"的“包裹”取出来
        val scanResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 对于 Android 13 (API 33) 及以上版本，使用新的、类型安全的方法
            // 我们需要明确告诉系统，我们想取出来的是一个 ScanResult 类型的包裹
            intent.getParcelableExtra(SCAN_RESULT, ScanResult::class.java)
        } else {
            // 对于旧版本，使用传统的方法
            @Suppress("DEPRECATION") // 告诉编译器，我们知道这个方法过时了，但为了兼容性还是要用
            intent.getParcelableExtra(SCAN_RESULT)
        }

        if(scanResult == null){
            //
            Toast.makeText(this, getString(R.string.scanner_get_result_fail), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            NekoCryptTheme {
                // 在这里显示我们的对话框
                // 当对话框请求关闭时，我们直接结束这个透明的 Activity
                ScannerDialog(scanResult,onDismissRequest = { finish() })
            }
        }
    }
}