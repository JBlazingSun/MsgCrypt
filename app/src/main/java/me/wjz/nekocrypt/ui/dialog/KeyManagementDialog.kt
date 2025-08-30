package me.wjz.nekocrypt.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.data.LocalDataStoreManager
import me.wjz.nekocrypt.data.rememberKeyArrayState
import me.wjz.nekocrypt.hook.rememberDataStoreState


@Composable
fun KeyManagementDialog(onDismissRequest: () -> Unit) {
    // 状态管理
    val dataStoreManager = LocalDataStoreManager.current
    val coroutineScope = rememberCoroutineScope()

    val keysFromDataStore: Array<String> by rememberKeyArrayState()
    val keys = remember { mutableStateListOf<String>() }

    // 数据库中key改变，同步到compose中
    LaunchedEffect(keysFromDataStore) {
        if(keys.toList()!=keysFromDataStore.toList()){
            keys.clear()
            keys.addAll(keysFromDataStore)
        }
    }

    // 当前正在使用的密钥
    var activeKey by rememberDataStoreState(SettingKeys.CURRENT_KEY, Constant.DEFAULT_SECRET_KEY)
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var keyToDelete by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    // ✨ 3. [核心] 创建一个通用的保存函数
    fun saveKeys(updatedKeys: SnapshotStateList<String>) {
        coroutineScope.launch {
            dataStoreManager.saveKeyArray(updatedKeys.toTypedArray())
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        // ✨ 2. [核心修正] 告诉Dialog不要使用平台默认的窄宽度
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // ✨ 3. [核心修正] 我们自己用Card来构建对话框的UI，并在这里设置宽度
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f), // ✨ 设置宽度为屏幕可用宽度的90%
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "密钥管理",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(0.6f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(keys) { key ->
                        KeyItem(
                            keyText = key,
                            isActive = key == activeKey,
                            onSetAsActive = { activeKey = key },
                            onCopy = { clipboardManager.setText(AnnotatedString(key)) },
                            onDelete = { keyToDelete = key }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = { showAddKeyDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "add", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                        Text("添加")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onDismissRequest) {
                        Text("完成")
                    }
                }
            }
        }
    }

    // --- 用于处理添加和删除的子对话框 ---
    if (showAddKeyDialog) {
        AddKeyDialog(
            onDismiss = { showAddKeyDialog = false },
            onAddKey = { newKey ->
                if (newKey.isNotBlank() && !keys.contains(newKey)) {
                    keys.add(newKey)
                    // ✨ 4. [核心] 添加后，立刻保存
                    saveKeys(keys)
                }
                showAddKeyDialog = false
            }
        )
    }

    if (keyToDelete != null) {
        DeleteConfirmDialog(
            onDismiss = { keyToDelete = null },
            onConfirm = {
                keys.remove(keyToDelete)
                if (activeKey == keyToDelete && keys.isNotEmpty()) {
                    activeKey = keys.first()
                }
                // ✨ 5. [核心] 删除后，立刻保存
                saveKeys(keys)
                keyToDelete = null
            }
        )
    }
}

/**
 * 列表里的单个密钥UI
 */
@Composable
private fun KeyItem(
    keyText: String,
    isActive: Boolean,
    onSetAsActive: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if(isActive) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
        )
    ){
        Row(
            modifier = Modifier
                .clickable(onClick = onSetAsActive)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "密钥图标",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = keyText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
            AnimatedVisibility(
                visible = isActive,
                enter = scaleIn() + fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "当前活动密钥",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "复制密钥")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除密钥", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}


/**
 * 添加新密钥的对话框
 */
@Composable
private fun AddKeyDialog(
    onDismiss: () -> Unit,
    onAddKey: (String) -> Unit
) {
    var newKey by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加新密钥") },
        text = {
            OutlinedTextField(
                value = newKey,
                onValueChange = { newKey = it },
                label = { Text("请输入密钥") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onAddKey(newKey) },
                enabled = newKey.isNotBlank() // 只有输入了内容才能点击
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}


/**
 * 删除确认对话框
 */
@Composable
private fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除这个密钥吗？此操作不可撤销。") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}