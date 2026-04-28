package me.wjz.nekocrypt.util

import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.wjz.nekocrypt.NekoCryptApp
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resumeWithException

/**
 * 封装图片、视频和文件的加密上传逻辑
 * 采用两步 OSS 上传：1. getParamsByAccount 获取凭证 → 2. POST 到 OSS
 */
object CryptoUploader {
    private const val TAG = "CryptoUploader"

    // ============ OSS 上传配置 ============
    // 第一步：获取上传凭证的 API
    private const val GET_PARAMS_URL =
        "https://api-takumi.mihoyogift.com/upload/outer/getParamsByAccount"
    // OSS 默认上传地址（API 返回 host 时优先使用 API 的）
    private const val DEFAULT_OSS_HOST =
        "https://plat-sh-operation-prod-upload-ugc.cn-shanghai.oss.aliyuncs.com/"
    // 上传成功后拼接文件外链的基础 URL
    private const val IMG_SRC_URL = "https://operation-upload.mihoyo.com"
    // 业务类型
    private const val BIZ = "mall-im-user"

    // 最大文件大小 50MB
    const val MAX_FILE_SIZE = 50L * 1024 * 1024

    // Cookie（写死，与测试脚本一致）
    const val COOKIE_STR = "_MHYUUID=5f763e16-2067-448a-9b34-7d96ef2442a9; MIHOYO_LOGIN_PLATFORM_LIFECYCLE_ID=92b4c88e90; DEVICEFP_SEED_ID=89a4c724ec8fdce7; DEVICEFP_SEED_TIME=1777346563865; DEVICEFP=38d817dba05f7; cookie_token_v2=v2_bgnw4o4ZdBI-3dBB3hxa3HCp_d_nKAQK-bb7ufa-_QUnFcPMiDWRLwArsDtU4EO7KEplSkZKS2NVuoe68Km3xohnCjPJrK41HBIeVDLwH3L6qNLGq59C6cTotYobzMYMNc5DypCvnOaGlZ_nPvfD.CAE=; account_mid_v2=0pc3m4rki2_mhy; account_id_v2=282706094; ltoken_v2=v2_OYQJoc9KRCiBE34GSim5h3gtGEgokSQGC-wiM5F2WEKxa5ugDGyE8qhhPjcG9blvmSFD5Nrwxh6HVpfg6D0ZlQ57wRdgdMPFTlE8U3so8GI3yF7b_r4dP8qro-FcwxaJJVwt2p36m4razyZbS52q.CAE=; ltmid_v2=0pc3m4rki2_mhy; ltuid_v2=282706094; cookie_token=92pot53yPHwWtgzgQoBLZYMzmdlj84ai8Ilo5w9O; account_id=282706094; ltoken=ZpkZC6nlSW7KtHZeR4eDGRVA7M64XhcoFaDTPPSG; ltuid=282706094; aliyungf_tc=340fe61fe20ba7dfb8b764396451a570555896311546ea33c3a7db4d76eda80f"

    // 1像素 GIF 伪装头
    val SINGLE_PIXEL_GIF_BUFFER: ByteArray =
        Base64.decode("R0lGODdhAQABAIABAP///wAAACwAAAAAAQABAAACAkQBADs=", Base64.DEFAULT)

    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    // ============ 第一步：获取上传凭证 ============

    /**
     * 调用 getParamsByAccount 接口，获取 OSS 上传所需的凭证
     */
    private fun getUploadParams(fileMd5: String, ext: String): Map<String, Any?> {
        Log.d(TAG, "[Step1] 开始获取上传凭证, md5=$fileMd5, ext=$ext, biz=$BIZ")

        val jsonBody = """{"md5":"$fileMd5","ext":"$ext","biz":"$BIZ","support_content_type":true}"""
        Log.d(TAG, "[Step1] 请求体: $jsonBody")

        val request = Request.Builder()
            .url(GET_PARAMS_URL)
            .header("Content-Type", "application/json")
            .header("Cookie", COOKIE_STR)
            .header("Origin", "https://webstatic.mihoyogift.com")
            .header("Referer", "https://webstatic.mihoyogift.com/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/147.0.0.0")
            .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()
        val bodyString = response.body.string() ?: throw IOException("获取凭证响应为空")

        val jsonObject = Json.parseToJsonElement(bodyString).jsonObject
        val retcode = jsonObject["retcode"]?.jsonPrimitive?.content?.toInt()
        if (retcode != 0) {
            Log.e(TAG, "[Step1] 获取凭证失败! retcode=$retcode, message=${jsonObject["message"]}")
            throw IOException("获取凭证失败: retcode=$retcode, message=${jsonObject["message"]}")
        }

        val data = jsonObject["data"]?.jsonObject ?: throw IOException("响应中缺少 data 字段")

        // 提取关键字段
        val oss = data["oss"]?.jsonObject ?: throw IOException("响应中缺少 data.oss 字段")
        val fileName = data["file_name"]?.jsonPrimitive?.content
            ?: throw IOException("响应中缺少 data.file_name 字段")

        val host = oss["host"]?.jsonPrimitive?.content ?: DEFAULT_OSS_HOST
        val policy = oss["policy"]?.jsonPrimitive?.content
        val signature = oss["signature"]?.jsonPrimitive?.content
        val accessid = oss["accessid"]?.jsonPrimitive?.content

        Log.d(TAG, "[Step1] 凭证获取成功!")
//        Log.d(TAG, "[Step1]   host: $host")
//        Log.d(TAG, "[Step1]   key: $fileName")
//        Log.d(TAG, "[Step1]   policy: $policy")
//        Log.d(TAG, "[Step1]   signature: $signature")
//        Log.d(TAG, "[Step1]   accessid: $accessid")

        // 解码 policy 看过期时间和限制条件
        try {
            val policyJson = String(Base64.decode(policy, Base64.DEFAULT))
            Log.d(TAG, "[Step1]   policy解码: $policyJson")
        } catch (_: Exception) {}

        return mapOf(
            "host" to host,
            "policy" to policy,
            "signature" to signature,
            "accessid" to accessid,
            // callback 不传，和测试脚本保持一致
            // "callback" to oss["callback"]?.jsonPrimitive?.content,
            "file_name" to fileName,
        )
    }

    // ============ 第二步：上传到 OSS ============

    /**
     * 将文件通过 multipart POST 上传到 OSS
     */
    private fun uploadToOss(
        params: Map<String, Any?>,
        fileData: ByteArray,
        fileName: String,
        ext: String,
        onProcess: (Int) -> Unit,
    ): String {
        val host = params["host"] as String
        val key = params["file_name"] as String

//        Log.d(TAG, "[Step2] 开始上传到 OSS")
//        Log.d(TAG, "[Step2]   目标: $host")
//        Log.d(TAG, "[Step2]   key: $key")
//        Log.d(TAG, "[Step2]   文件名: $fileName")
//        Log.d(TAG, "[Step2]   数据大小: ${fileData.size} bytes")

        // 构造 multipart 表单
        val multipartBuilder = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", key)
            .addFormDataPart("policy", params["policy"] as String)
            .addFormDataPart("signature", params["signature"] as String)
            .addFormDataPart("OSSAccessKeyId", params["accessid"] as String)
            .addFormDataPart("success_action_status", "200")
            .addFormDataPart("x-oss-content-type", "image/$ext")

        // callback 不传，和测试脚本保持一致
        // (params["callback"] as? String)?.let { callback ->
        //     if (callback.isNotBlank()) {
        //         multipartBuilder.addFormDataPart("callback", callback)
        //     }
        // }

        // 文件部分，带进度
        val fileRequestBody = ProgressRequestBody(
            data = fileData,
            contentType = "image/$ext".toMediaTypeOrNull(),
            onProcess = onProcess
        )
        multipartBuilder.addFormDataPart("file", fileName, fileRequestBody)

        val request = Request.Builder()
            .url(host)
            .header("Origin", "https://webstatic.mihoyogift.com")
            .header("Referer", "https://webstatic.mihoyogift.com/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/147.0.0.0")
            .post(multipartBuilder.build())
            .build()

        Log.d(TAG, "[Step2] 正在发送 POST 请求到 OSS...")
        val response = client.newCall(request).execute()
        val respBody = response.body?.string()

        if (!response.isSuccessful) {
            Log.e(TAG, "[Step2] OSS 上传失败! 状态码: ${response.code}, 响应: $respBody")
            throw IOException("OSS 上传失败，状态码: ${response.code}，响应: $respBody")
        }

        // 上传成功，拼接文件 URL
        val fileUrl = "$IMG_SRC_URL/$key"
        Log.d(TAG, "[Step2] 上传成功! 状态码: ${response.code}")
        Log.d(TAG, "[Step2] 响应体: $respBody")
        Log.d(TAG, "[Step2] 文件外链: $fileUrl")
        return fileUrl
    }

    // ============ 对外接口 ============

    /**
     * 上传字节数组（当前主用版本）
     * 流程：加密 → GIF伪装 → 计算MD5 → 获取凭证 → 上传OSS → 返回URL
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun upload(
        fileBytes: ByteArray,
        fileName: String = "",
        encryptionKey: String,
        onProcess: (progress: Int) -> Unit,
    ): NCFileProtocol {
        Log.d(TAG, "========== 开始上传(字节数组) ==========")
        Log.d(TAG, "原始文件: $fileName, 大小: ${fileBytes.size} bytes, 类型: ${if (fileBytes.isImage()) "图片" else "文件"}")

        return suspendCancellableCoroutine { continuation ->
            try {
                // 1. 加密 + GIF 伪装
                Log.d(TAG, "加密中...")
                val encryptedBytes = CryptoManager.encrypt(fileBytes, encryptionKey)
                val payload = SINGLE_PIXEL_GIF_BUFFER + encryptedBytes
                Log.d(TAG, "加密完成, 伪装后大小: ${payload.size} bytes (含GIF头 ${SINGLE_PIXEL_GIF_BUFFER.size} bytes)")

                // 2. 计算 MD5
                val fileMd5 = computeMd5(payload)
                Log.d(TAG, "payload MD5: $fileMd5")

                // 3. 获取上传凭证
                val params = getUploadParams(fileMd5, "gif")

                // 4. 上传到 OSS
                val uploadFileName = "$fileName.gif"
                val fileUrl = uploadToOss(params, payload, uploadFileName, "gif", onProcess)

                // 5. 构造返回结果
                if (fileUrl.isNotBlank()) {
                    val result = NCFileProtocol(
                        url = fileUrl,
                        size = fileBytes.size.toLong(),
                        name = fileName,
                        encryptionKey = encryptionKey,
                        type = if (fileBytes.isImage()) NCFileType.IMAGE else NCFileType.FILE
                    )
                    Log.d(TAG, "========== 上传完成 ==========")
                    Log.d(TAG, "结果: url=${result.url}, name=${result.name}, size=${result.size}, type=${result.type}")
                    continuation.resume(result, null)
                } else {
                    Log.e(TAG, "构造文件 URL 失败: fileUrl 为空")
                    continuation.resumeWithException(IOException("构造文件 URL 失败"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "========== 上传失败 ==========", e)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
    }

    /**
     * 上传 Uri 文件（流式版本，目前未使用但保留）
     * 流程：读取+加密到内存 → GIF伪装 → 计算MD5 → 获取凭证 → 上传OSS → 返回URL
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun upload(
        uri: Uri,
        fileName: String,
        encryptionKey: String,
        onProcess: (progress: Int) -> Unit,
    ): NCFileProtocol {
        val fileSize = getFileSize(uri)
        Log.d(TAG, "========== 开始上传(Uri) ==========")
        Log.d(TAG, "URI: $uri, 文件名: $fileName, 大小: $fileSize bytes")

        val inputStream = NekoCryptApp.instance.contentResolver.openInputStream(uri)
            ?: throw IOException("Failed to open input stream from URI")

        // 先读取到内存（因为需要先计算MD5再上传）
        val fileBytes = inputStream.use { it.readBytes() }
        Log.d(TAG, "文件读取完成, 实际大小: ${fileBytes.size} bytes")

        // 加密 + GIF 伪装
        val encryptedBytes = CryptoManager.encrypt(fileBytes, encryptionKey)
        val payload = SINGLE_PIXEL_GIF_BUFFER + encryptedBytes

        // 计算 MD5
        val fileMd5 = computeMd5(payload)

        // 获取凭证 + 上传
        return suspendCancellableCoroutine { continuation ->
            try {
                val params = getUploadParams(fileMd5, "gif")
                val uploadFileName = "$fileName.gif"

                val fileUrl = uploadToOss(params, payload, uploadFileName, "gif", onProcess)

                if (fileUrl.isNotBlank()) {
                    val result = NCFileProtocol(
                        url = fileUrl,
                        size = fileSize,
                        name = fileName,
                        encryptionKey = encryptionKey,
                        type = if (isFileImage(uri)) NCFileType.IMAGE else NCFileType.FILE
                    )
                    Log.d(TAG, "========== 上传完成 ==========")
                    Log.d(TAG, "结果: url=${result.url}, name=${result.name}, size=${result.size}, type=${result.type}")
                    continuation.resume(result, null)
                } else {
                    Log.e(TAG, "构造文件 URL 失败: fileUrl 为空")
                    continuation.resumeWithException(IOException("构造文件 URL 失败"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "========== 上传失败 ==========", e)
                if (continuation.isActive) continuation.resumeWithException(e)
            }
        }
    }

    // ============ 工具方法 ============

    /**
     * 计算字节数组的 MD5 哈希（小写十六进制）
     */
    private fun computeMd5(data: ByteArray): String {
        val digest = MessageDigest.getInstance("MD5")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}

/**
 * 带进度回调的 RequestBody
 */
private class ProgressRequestBody(
    private val data: ByteArray,
    private val contentType: okhttp3.MediaType?,
    private val onProcess: (Int) -> Unit,
) : RequestBody() {
    override fun contentType(): okhttp3.MediaType? = contentType
    override fun contentLength(): Long = data.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        var bytesWritten = 0L
        val bufferSize = 8 * 1024

        data.inputStream().use { inputStream ->
            val buffer = ByteArray(bufferSize)
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                bytesWritten += read
                val progress = (100 * bytesWritten / totalBytes).toInt()
                onProcess(progress)
            }
        }
    }
}
