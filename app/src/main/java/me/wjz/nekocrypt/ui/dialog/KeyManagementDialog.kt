package me.wjz.nekocrypt.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import me.wjz.nekocrypt.data.rememberKeyArrayState

@Composable
fun KeyManagementDialog(onDismissRequest: () -> Unit) {

    // 状态管理
    val keys: Array<String> by rememberKeyArrayState()
}