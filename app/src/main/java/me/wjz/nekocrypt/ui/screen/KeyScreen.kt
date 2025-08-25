package me.wjz.nekocrypt.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.hook.rememberDataStoreState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager

@Composable
fun KeyScreen(modifier: Modifier = Modifier) {
    // 状态管理
    var currentKey by rememberDataStoreState(SettingKeys.CURRENT_KEY, Constant.DEFAULT_SECRET_KEY)
    // 2. [保留] 这些是临时的 UI 状态，不需要持久化，所以继续使用 remember
    var isKeyVisible by remember { mutableStateOf(false) }
    var showCustomKeyDialog by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() } // 假设父级有 Scaffold 提供 SnackbarHost
    val scope = rememberCoroutineScope()


    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("这里是密钥页面", style = MaterialTheme.typography.headlineMedium)
    }
}