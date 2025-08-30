package me.wjz.nekocrypt.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.wjz.nekocrypt.Constant.DEFAULT_SECRET_KEY
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys.CURRENT_KEY
import me.wjz.nekocrypt.hook.rememberDataStoreState
import me.wjz.nekocrypt.ui.dialog.KeyManagementDialog
import me.wjz.nekocrypt.util.CryptoManager
import me.wjz.nekocrypt.util.CryptoManager.appendNekoTalk

@Composable
fun CryptoScreen(modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isEncryptMode by remember { mutableStateOf(true) }//当前是加密or解密
    //获取当前密钥，没有就是默认密钥
    val secretKey: String by rememberDataStoreState(CURRENT_KEY, DEFAULT_SECRET_KEY)
    val decryptFailed = stringResource(id = R.string.crypto_decrypt_fail)//解密错误的text。
    var isDecryptFailed by remember { mutableStateOf(false) }
    // 新增：用于统计的状态
    var charCount by remember { mutableStateOf(0) }
    var elapsedTime by remember { mutableStateOf(0L) }
    // 新增一个状态，用来控制密钥管理对话框的显示和隐藏
    var showKeyDialog by remember { mutableStateOf(false) }
    //自动加解密
    LaunchedEffect(inputText) {
        if (inputText.isEmpty()) {
            outputText = ""
            // 输入为空时重置统计数据
            charCount = 0
            elapsedTime = 0L
            return@LaunchedEffect
        }

        val startTime = System.currentTimeMillis() // 记录开始时间
        var ciphertextCharCount = 0 // 临时变量，用于存储密文长度

        val resultMsg = if (CryptoManager.containsCiphertext(inputText)) {
            //走解密
            isEncryptMode = false
            ciphertextCharCount = inputText.length // 解密时，输入框内容就是密文
            CryptoManager.decrypt(inputText, secretKey)
        } else {
            isEncryptMode = true
            val ciphertext = CryptoManager.encrypt(inputText, secretKey).appendNekoTalk()
            ciphertextCharCount = ciphertext.length // 加密时，输出结果是密文
            ciphertext
        }

        val endTime = System.currentTimeMillis() // 记录结束时间
        elapsedTime = endTime - startTime // 计算耗时

        isDecryptFailed = resultMsg == null
        outputText = resultMsg ?: decryptFailed
        charCount = ciphertextCharCount // 更新状态
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 密钥选择器
        KeySelector(
            selectedKeyName = secretKey,
            onClick = {
                // 当点击时，将状态设置为true，以显示对话框
                showKeyDialog = true
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 输入文本框
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
//                .height(180.dp),这里不设置固定高度
                .verticalScroll(rememberScrollState()),
            minLines = 1,//控制默认的最小行数
            maxLines = 6,//控制最大行数
            label = { Text(stringResource(id = R.string.crypto_input_label)) },
            placeholder = { Text(stringResource(id = R.string.crypto_input_placeholder)) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Rounded.Notes,
                    contentDescription = stringResource(id = R.string.crypto_input_icon_desc)
                )
            },
            // 右方的辅助按钮
            trailingIcon = {
                Row {
                    // 粘贴按钮
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { inputText += it }//这里+=
                        Toast.makeText(
                            context,
                            context.getString(R.string.crypto_pasted_from_clipboard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = stringResource(id = R.string.crypto_paste_icon_desc)
                        )
                    }
                    // 清空按钮，仅在有输入时显示
                    AnimatedVisibility(visible = inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = stringResource(id = R.string.crypto_clear_input_icon_desc)
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 输出结果区域
        // 使用 AnimatedVisibility，当有输出时，这个区域会平滑地淡入
        AnimatedVisibility(
            visible = outputText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            OutlinedTextField(
                value = outputText,
                onValueChange = {}, // 输出框通常是只读的
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth(),
//                    .height(180.dp),
                minLines = 1,
                maxLines = 6,
                isError = isDecryptFailed,
                label = { Text(stringResource(if (isEncryptMode) R.string.crypto_result_label_encrypted else R.string.crypto_result_label_decrypted)) },
                // 右下角的复制按钮
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(outputText))
                        Toast.makeText(
                            context,
                            context.getString(R.string.crypto_copied_to_clipboard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(id = R.string.crypto_copy_result_icon_desc)
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 统计信息区域
        AnimatedVisibility(
            visible = outputText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            CryptoStats(
                charCount = charCount,
                elapsedTime = elapsedTime
            )
        }

        if(showKeyDialog){
            KeyManagementDialog(onDismissRequest = { showKeyDialog = false })
        }
    }
}


/**
 * 一个用于展示统计信息（字符数和耗时）的组件。
 */
@Composable
private fun CryptoStats(
    charCount: Int,
    elapsedTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 无阴影，更轻量
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 字符总数统计
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.crypto_stats_char_count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = charCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 处理耗时统计
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.crypto_stats_time_elapsed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.crypto_stats_time_ms, elapsedTime),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * 一个用于展示和选择密钥的自定义组件。
 */
@Composable
fun KeySelector(
    selectedKeyName: String,
    onClick: () -> Unit
) {
    // 将形状定义为一个变量，方便复用
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = stringResource(id = R.string.crypto_key_icon_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.crypto_current_key_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                    Text(
                        text = selectedKeyName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(id = R.string.crypto_select_key_icon_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
