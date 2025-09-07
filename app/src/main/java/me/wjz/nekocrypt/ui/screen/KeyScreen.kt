package me.wjz.nekocrypt.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.dianming.phoneapp.MyAccessibilityService
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import me.wjz.nekocrypt.AppRegistry
import me.wjz.nekocrypt.Constant.DEFAULT_SECRET_KEY
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys.CURRENT_KEY
import me.wjz.nekocrypt.data.rememberCustomAppState
import me.wjz.nekocrypt.hook.rememberDataStoreState
import me.wjz.nekocrypt.service.handler.ChatAppHandler
import me.wjz.nekocrypt.ui.SettingsHeader
import me.wjz.nekocrypt.ui.SwitchSettingItem
import me.wjz.nekocrypt.ui.dialog.KeyManagementDialog

@Composable
fun KeyScreen(modifier: Modifier = Modifier) {
    // 状态管理
    var currentKey by rememberDataStoreState(CURRENT_KEY, DEFAULT_SECRET_KEY)
    var showKeyDialog by remember { mutableStateOf(false) }     //控制密钥管理对话框的显示和隐藏
    // 自定义APP列表
    val customApps by rememberCustomAppState()
    val context = LocalContext.current

    // 这个本地状态将作为“状态提升”的数据源，控制着开关的UI
    var isScannerSwitchOn by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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

        // 支持的应用
        item {
            SettingsHeader(stringResource(R.string.key_screen_supported_app))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }

        item{
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                // 在 Card 内部使用 Column 来垂直排列我们的 App 列表项
                Column(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp), // 给上下一点内边距
                    verticalArrangement = Arrangement.spacedBy(12.dp) // 垂直项间距
                ) {
                    AppRegistry.allHandlers.forEach { handler ->
                        SupportedAppItem(handler = handler)
                    }
                }
            }
        }
        // 自定义应用
        item {
            SettingsHeader(stringResource(R.string.key_screen_custom_app))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 根据列表是否为空，显示不同的内容
                    if (customApps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.key_screen_no_custom_app_configured),
                            // 为了让单行文本在卡片内居中，我们给它一个 Modifier
                            modifier = Modifier.fillMaxWidth()
                                .padding(vertical = 8.dp) // 给一点垂直padding，避免太贴近按钮
                                .align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // 遍历自定义应用列表，显示每一项
                        customApps.forEach { customHandler ->
                            SupportedAppItem(handler = customHandler)
                        }
                    }
                }
            }
        }

        item{
            // 这里的开关将负责检查权限并启动扫描悬浮窗
            SwitchSettingItem(
                title = stringResource(R.string.enable_scanner_mode),
                subtitle = stringResource(R.string.enable_scanner_mode_description),
                isChecked = isScannerSwitchOn, // 开关状态由外部传入
                onCheckedChange = { isChecked -> // 开关的行为由外部定义
                    // 检查悬浮窗权限
                    if (isChecked && !Settings.canDrawOverlays(context)) {
                        Toast.makeText(context, context.getString(R.string.please_grant_overlay_permission), Toast.LENGTH_LONG).show()
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        context.startActivity(intent)
                    } else {
                        // 更新UI状态
                        isScannerSwitchOn = isChecked
                        // 根据开关状态，发送不同的指令给服务
                        val action = if (isChecked) {
                            MyAccessibilityService.ACTION_SHOW_SCANNER
                        } else {
                            MyAccessibilityService.ACTION_HIDE_SCANNER
                        }
                        val intent = Intent(context, MyAccessibilityService::class.java).apply {
                            this.action = action
                        }
                        context.startService(intent)
                    }
                },
                // 我们还给它加了个漂亮的图标
                icon = {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.enable_scanner_mode)
                    )
                }
            )
        }
    }

    if(showKeyDialog){
        KeyManagementDialog(onDismissRequest = { showKeyDialog = false })
    }
}

@Composable
fun SupportedAppItem(handler: ChatAppHandler){
    var isEnabled by rememberDataStoreState(booleanPreferencesKey("app_enabled_${handler.packageName}"),
        defaultValue = true
    )
    var showHandlerInfoDialog by remember { mutableStateOf(false) }    //控制是否展示handler详细信息

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
            Log.e(NekoCryptApp.TAG, e.toString())
        }
    }

    if (showHandlerInfoDialog) {
        AppHandlerInfoDialog(
            handler = handler,
            onDismissRequest = { showHandlerInfoDialog = false }
        )
    }

    Card(
        onClick = { showHandlerInfoDialog = true },
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
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
                    handler.name + if (!isAppInstalled) " — ${stringResource(R.string.not_installed)}" else "",
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
                onCheckedChange = {
                    isEnabled = it
                    //Log.d(NekoCryptApp.TAG, "包${handler.packageName}监听状态：$it")
                },
                // ✨ 如果App没安装，开关就禁用
                enabled = isAppInstalled
            )
        }
    }
}

@Composable
private fun AppHandlerInfoDialog(
    handler: ChatAppHandler,
    onDismissRequest: () -> Unit,
){
    AlertDialog(
        onDismissRequest = onDismissRequest,
        // 标题
        title = { Text(text = "${handler.name} - 配置详情") },
        // 内容
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(label = stringResource(R.string.key_screen_supported_app_input_id), value = handler.inputId)
                InfoRow(label = stringResource(R.string.key_screen_supported_app_send_btn_id), value = handler.sendBtnId)
                InfoRow(label = stringResource(R.string.key_screen_supported_app_message_text_id), value = handler.messageTextId)
                InfoRow(label = stringResource(R.string.key_screen_supported_app_message_list_class_name), value = handler.messageListClassName)
            }
        },
        // 确认按钮
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * ✨ “魔法大改造”后的 InfoRow！
 */
@Composable
private fun InfoRow(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val hasCopyHint = stringResource(R.string.has_copy)
    Column {
        // 标签
        Text(
            text = label,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        // 内容和复制按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用 Surface 包裹，创造代码块效果
            Surface(
                onClick = {
                    if(value.isNotEmpty()){
                        clipboardManager.setText(AnnotatedString(value))
                        // 显示一个短暂的提示
                        Toast.makeText(context, hasCopyHint, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp, // 增加一点色调深度
            ) {
                Text(
                    text = value.ifEmpty { "N/A" }, // 如果值为空，显示 N/A
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace, // ✨ 使用等宽字体！
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}