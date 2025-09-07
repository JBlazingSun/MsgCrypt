package me.wjz.nekocrypt.ui.dialog

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.service.handler.ChatAppHandler


@Composable
fun AppHandlerInfoDialog(
    handler: ChatAppHandler,
    onDismissRequest: () -> Unit,
){
    AlertDialog(
        onDismissRequest = onDismissRequest,
        // 标题
        title = { Text(text = "${handler.name} - 配置详情") },
        // 内容
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(label = stringResource(R.string.key_screen_supported_app_input_id), value = handler.inputId)
                InfoRow(label = stringResource(R.string.key_screen_supported_app_send_btn_id), value = handler.sendBtnId)
                InfoRow(label = stringResource(R.string.key_screen_supported_app_message_text_id), value = handler.messageTextId)
                InfoRow(label = stringResource(R.string.key_screen_supported_app_message_list_class_name), value = handler.messageListClassName)
            }
        },
        // 确认按钮
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * ✨ “魔法大改造”后的 InfoRow！
 */
@Composable
private fun InfoRow(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val hasCopyHint = stringResource(R.string.has_copy)
    Column {
        // 标签
        Text(
            text = label,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 内容和复制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用 Surface 包裹，创造代码块效果
            Surface(
                onClick = {
                    if(value.isNotEmpty()){
                        clipboardManager.setText(AnnotatedString(value))
                        // 显示一个短暂的提示
                        Toast.makeText(context, hasCopyHint, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp, // 增加一点色调深度
            ) {
                Text(
                    text = value.ifEmpty { "N/A" }, // 如果值为空，显示 N/A
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace, // ✨ 使用等宽字体！
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}