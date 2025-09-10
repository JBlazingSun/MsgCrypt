package me.wjz.nekocrypt.ui.dialog

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
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.activity.FoundNodeInfo
import me.wjz.nekocrypt.ui.activity.MessageListScanResult
import me.wjz.nekocrypt.ui.activity.ScanResult

/**
 * 悬浮扫描按钮点击后显示的对话框 Composable (V3 协同版)。
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
                .fillMaxWidth(0.9f) // 使用屏幕宽度的90%
                .padding(16.dp)
                .heightIn(max = screenHeight * 0.85f), // 最大高度为屏幕的85%
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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

                // 使用可滚动的 LazyColumn 来展示所有区块
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
                    // ✨ 使用全新的方式来展示消息列表和其内部的消息
                    item {
                        MessageListSection(
                            title = stringResource(R.string.scanner_dialog_section_msg_list),
                            lists = scanResult.foundMessageLists
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 底部按钮 ---
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
 * ✨ 全新：用于显示消息列表区块的 Composable
 * 它会先展示列表容器的信息，然后嵌套展示其内部的消息文本。
 */
@Composable
private fun MessageListSection(title: String, lists: List<MessageListScanResult>) {
    if (lists.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = "$title (${lists.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 遍历每个找到的“房子”
            lists.forEach { listResult ->
                // 1. 先显示“房子”本身的信息
                NodeInfoCard(nodeInfo = listResult.listContainerInfo)
                Spacer(modifier = Modifier.height(8.dp))

                // 2. 如果“房子”里有“居民”，就缩进一点，然后展示他们
                if (listResult.messageTexts.isNotEmpty()) {
                    Column(modifier = Modifier.padding(start = 16.dp)) {
                        Text(
                            text = "└─ ${stringResource(R.string.scanner_dialog_section_msg_text)} (${listResult.messageTexts.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        listResult.messageTexts.forEach { textNode ->
                            NodeInfoCard(nodeInfo = textNode)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
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
            // resourceId 不为空白时才显示
            if (nodeInfo.resourceId?.isNotBlank() == true) {
                InfoRow(
                    label = stringResource(R.string.scanner_dialog_card_id),
                    value = nodeInfo.resourceId
                )
            }

            // className 总是显示，因为它在数据类里不是可空的
            InfoRow(
                label = stringResource(R.string.scanner_dialog_card_class),
                value = nodeInfo.className
            )

            // text 不为空白时才显示
            if (nodeInfo.text?.isNotBlank() == true) {
                InfoRow(
                    label = stringResource(R.string.scanner_dialog_card_text),
                    value = nodeInfo.text
                )
            }

            // contentDescription 不为空白时才显示
            if (nodeInfo.contentDescription?.isNotBlank() == true) {
                InfoRow(
                    label = stringResource(R.string.scanner_dialog_card_desc),
                    value = nodeInfo.contentDescription
                )
            }
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
                if (value.isNotEmpty()) {
                    clipboardManager.setText(AnnotatedString(value))
                    Toast.makeText(context, "'$value' $hasCopyHint", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

