package me.wjz.nekocrypt.ui.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.wjz.nekocrypt.R

/**
 * 悬浮扫描按钮点击后显示的对话框 Composable。
 */
@Composable
fun ScannerDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        // 标题
        title = { Text(text = stringResource(R.string.scanner_dialog_title)) },
        // 内容
        text = {
            Text(text = "测试")
        },
        // 确认按钮
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.accept))
            }
        }
    )
}