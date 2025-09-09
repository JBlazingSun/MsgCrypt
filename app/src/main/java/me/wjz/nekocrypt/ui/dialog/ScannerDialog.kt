package me.wjz.nekocrypt.ui.dialog

import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val contentDescription: String?,
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
    val foundMessageListNodes: List<FoundNodeInfo>,
) : Parcelable

/**
 * 悬浮扫描按钮点击后显示的对话框 Composable。
 */
@Composable
fun ScannerDialog(
    scanResult: ScanResult,
    onDismissRequest: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp).heightIn(screenHeight *0.9f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                //  总标题
                Text(
                    text = stringResource(R.string.scanner_dialog_title),
                    style = MaterialTheme.typography.headlineSmall
                )

                // 显示当前应用的包名和名称
                Text(
                    text = "${scanResult.name} (${scanResult.packageName})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    item {
                        ScanResultSection(
                            title = stringResource(R.string.scanner_dialog_section_input),
                            nodes = scanResult.foundInputNodes
                        )
                    }
                    item {
                        ScanResultSection(
                            title = stringResource(R.string.scanner_dialog_section_send_btn),
                            nodes = scanResult.foundSendBtnNodes
                        )
                    }
                    item {
                        ScanResultSection(
                            title = stringResource(R.string.scanner_dialog_section_msg_list),
                            nodes = scanResult.foundMessageListNodes
                        )
                    }
                    item {
                        ScanResultSection(
                            title = stringResource(R.string.scanner_dialog_section_msg_text),
                            nodes = scanResult.foundMessageTextNodes
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 3. 底部按钮 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.accept))
                    }
                }

            }
        }
    }
}

/**
 * 用于显示一个扫描结果区块（例如 "找到的输入框"）的 Composable。
 * 如果节点列表为空，则什么都不显示。
 */
@Composable
private fun ScanResultSection(title: String, nodes: List<FoundNodeInfo>) {
    // 只有当列表里有内容时，才显示这个区块
    if (nodes.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            // 区块标题
            Text(
                text = "$title (${nodes.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 遍历并显示每个节点的卡片
            nodes.forEach { node ->
                NodeInfoCard(nodeInfo = node)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * 用于显示单个节点详细信息的卡片 Composable。
 */
@Composable
private fun NodeInfoCard(nodeInfo: FoundNodeInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 模仿 AppHandlerInfoDialog 的 InfoRow 样式来显示信息
            InfoRow(
                label = stringResource(R.string.scanner_dialog_card_id),
                value = nodeInfo.resourceId ?: "N/A"
            )
            InfoRow(
                label = stringResource(R.string.scanner_dialog_card_class),
                value = nodeInfo.className
            )
            InfoRow(
                label = stringResource(R.string.scanner_dialog_card_text),
                value = nodeInfo.text ?: "N/A"
            )
            InfoRow(
                label = stringResource(R.string.scanner_dialog_card_desc),
                value = nodeInfo.contentDescription ?: "N/A"
            )
        }
    }
}

/**
 * 用于在卡片内显示一行“标签: 内容”信息，并支持点击复制。
 */
@Composable
private fun InfoRow(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val hasCopyHint = stringResource(R.string.has_copy)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标签
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(64.dp) // 给标签一个固定宽度，让内容对齐
        )
        // 内容
        Surface(
            onClick = {
                if (value.isNotEmpty() && value != "N/A") {
                    clipboardManager.setText(AnnotatedString(value))
                    Toast.makeText(context, "'$value' $hasCopyHint", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = value.ifEmpty { "N/A" },
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
