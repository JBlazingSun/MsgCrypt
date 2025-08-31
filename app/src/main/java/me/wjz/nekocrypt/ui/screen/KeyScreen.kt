package me.wjz.nekocrypt.ui.screen

import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import me.wjz.nekocrypt.AppRegistry
import me.wjz.nekocrypt.Constant.DEFAULT_SECRET_KEY
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys.CURRENT_KEY
import me.wjz.nekocrypt.hook.rememberDataStoreState
import me.wjz.nekocrypt.service.handler.ChatAppHandler
import me.wjz.nekocrypt.ui.dialog.KeyManagementDialog

@Composable
fun KeyScreen(modifier: Modifier = Modifier) {
    // 状态管理
    var currentKey by rememberDataStoreState(CURRENT_KEY, DEFAULT_SECRET_KEY)
    // 新增一个状态，用来控制密钥管理对话框的显示和隐藏
    var showKeyDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 密钥选择器
        item {
            KeySelector(
                selectedKeyName = currentKey,
                onClick = {
                    // 当点击时，将状态设置为true，以显示对话框
                    showKeyDialog = true
                }
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }

        items(AppRegistry.allHandlers) { handler ->
            // 将 handler 实例传递给我们的列表项 Composable
            SupportedAppItem(handler = handler)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if(showKeyDialog){
        KeyManagementDialog(onDismissRequest = { showKeyDialog = false })
    }
}

@Composable
fun SupportedAppItem(handler: ChatAppHandler){
    var isEnabled by rememberDataStoreState(
        booleanPreferencesKey("app_enabled_${handler.packageName}"),
        defaultValue = true
    )
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<Drawable?>(null) }
    var isAppInstalled by remember { mutableStateOf(false) }

    // 尝试获取应用图标
    LaunchedEffect(handler.packageName) {
        try{
            appIcon = context.packageManager.getApplicationIcon(handler.packageName)
            isAppInstalled = true
        }catch (e: PackageManager.NameNotFoundException) {
            appIcon = null
            isAppInstalled = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if(isAppInstalled&& appIcon!=null){
                Image(
                    // 用Google的Accompanist
                    painter = rememberDrawablePainter(drawable = appIcon),
                    contentDescription = "${handler.name} 图标", // ✨ 使用 handler.name
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "应用未安装", modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // 然后放APP名和包名
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    handler.name + if (!isAppInstalled) " —— ${stringResource(R.string.not_installed)}" else "",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    handler.packageName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // 右边放开关
            Switch(
                checked = isEnabled,
                onCheckedChange = { isEnabled = it },
                // ✨ 如果App没安装，开关就禁用
                enabled = isAppInstalled
            )
        }
    }
}