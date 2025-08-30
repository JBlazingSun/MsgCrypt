package me.wjz.nekocrypt.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import me.wjz.nekocrypt.Constant.DEFAULT_SECRET_KEY
import me.wjz.nekocrypt.SettingKeys.CURRENT_KEY
import me.wjz.nekocrypt.hook.rememberDataStoreState
import me.wjz.nekocrypt.ui.dialog.KeyManagementDialog

@Composable
fun KeyScreen(modifier: Modifier = Modifier) {
    // 状态管理
    var currentKey by rememberDataStoreState(CURRENT_KEY, DEFAULT_SECRET_KEY)
    // 2. [保留] 这些是临时的 UI 状态，不需要持久化，所以继续使用 remember
    var isKeyVisible by remember { mutableStateOf(false) }
    var showCustomKeyDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() } // 假设父级有 Scaffold 提供 SnackbarHost
    val scope = rememberCoroutineScope()
    // 新增一个状态，用来控制密钥管理对话框的显示和隐藏
    var showKeyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 密钥选择器
        KeySelector(
            selectedKeyName = currentKey,
            onClick = {
                // 当点击时，将状态设置为true，以显示对话框
                showKeyDialog = true
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

    }

    if(showKeyDialog){
        KeyManagementDialog(onDismissRequest = { showKeyDialog = false })
    }
}