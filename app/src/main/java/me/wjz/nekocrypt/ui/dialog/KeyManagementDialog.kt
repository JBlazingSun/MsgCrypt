package me.wjz.nekocrypt.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.data.LocalDataStoreManager
import me.wjz.nekocrypt.data.rememberKeyArrayState
import me.wjz.nekocrypt.hook.rememberDataStoreState
import java.nio.file.WatchEvent


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

    // 我们使用 AlertDialog 作为基础，因为它提供了标准的对话框样式
     AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("密钥管理") },
        // 在 text 区域，我们放入自己的滚动列表
        text = {
            LazyColumn(
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
        },
        // 在 confirmButton 区域，我们放入我们的操作按钮
        confirmButton = {
            Row {
                OutlinedButton(onClick = { showAddKeyDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                    Text("添加")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onDismissRequest) {
                    Text("完成")
                }
            }
        }
    )

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
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if(isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ){
        Row(
            modifier = Modifier.clickable(onClick = onSetAsActive).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
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
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
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

