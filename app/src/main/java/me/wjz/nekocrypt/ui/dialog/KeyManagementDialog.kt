package me.wjz.nekocrypt.ui.dialog

import androidx.compose.runtime.Composable
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.SettingKeys.ALL_THE_KEYS
import me.wjz.nekocrypt.hook.rememberDataStoreState

@Composable
fun KeyManagementDialog(onDismissRequest: () -> Unit) {

    // 状态管理
    val keys  by rememberDataStoreState(ALL_THE_KEYS, Constant.DEFAULT_SECRET_KEY)
}