package me.wjz.nekocrypt.ui.dialog

import android.os.Parcelable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.parcelize.Parcelize
import me.wjz.nekocrypt.R

/**
 * 一个用于封装单个被找到的节点信息的数据类。
 * @param className 节点的类名 (e.g., "android.widget.EditText")。
 * @param resourceId 节点的资源 ID (e.g., "com.tencent.mm:id/input_editor")，可能为空。
 * @param text 节点的文本内容，可能为空。
 * @param contentDescription 节点的内容描述（常用于无障碍），可能为空。
 */
@Parcelize
data class FoundNodeInfo(
    val className: String,
    val resourceId: String?,
    val text: String?,
    val contentDescription: String?
) : Parcelable

/**
 * 一个用于封装扫描结果的数据类，可以通过 Intent 传递。
 * @param packageName 当前处理器的包名。
 * @param name 当前处理器的可读名称 (e.g., "xx聊天")。
 * @param foundInputNodes 扫描到的所有可能的输入框节点列表。
 * @param foundSendBtnNodes 扫描到的所有可能的发送按钮节点列表。
 * @param foundMessageTextNodes 扫描到的所有可能的消息文本节点列表。
 * @param foundMessageListNodes 扫描到的所有可能的消息列表容器节点列表。
 */
@Parcelize
data class ScanResult(
    val packageName: String,
    val name: String,
    val foundInputNodes: List<FoundNodeInfo>,
    val foundSendBtnNodes: List<FoundNodeInfo>,
    val foundMessageTextNodes: List<FoundNodeInfo>,
    val foundMessageListNodes: List<FoundNodeInfo>
) : Parcelable

/**
 * 悬浮扫描按钮点击后显示的对话框 Composable。
 */
@Composable
fun ScannerDialog(
    scanResult: ScanResult,
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