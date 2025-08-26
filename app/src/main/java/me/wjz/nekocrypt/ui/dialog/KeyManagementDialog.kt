package me.wjz.nekocrypt.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalClipboardManager
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

    var currentKey by rememberDataStoreState(SettingKeys.CURRENT_KEY, Constant.DEFAULT_SECRET_KEY)
    var showAddKeyDialog by remember { mutableStateOf(false) }
    var keyToDelete by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    // ✨ 3. [核心] 创建一个通用的保存函数
    fun saveKeys(updatedKeys: SnapshotStateList<String>) {
        coroutineScope.launch {
            dataStoreManager.saveKeyArray(updatedKeys.toTypedArray())
        }
    }
}