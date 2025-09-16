package me.wjz.nekocrypt.util

import me.wjz.nekocrypt.R
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * 加密工具类，有相关的加密算法。
 */
object CryptoManager {

    private const val ALGORITHM = "AES"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256 // AES-256
    const val IV_LENGTH_BYTES = 16  // GCM 推荐的IV长度是12，为了该死的兼容改成16
    const val TAG_LENGTH_BITS = 128 // GCM 推荐的认证标签长度

    // 下面是一些映射表
    private val STEALTH_ALPHABET = (0xFE00..0xFE0F).map { it.toChar() }.joinToString("")

    /**
     * 为了高效解码，预先创建一个从“猫语”字符到其在字母表中索引位置的映射。
     * 这是一个关键的性能优化。
     */
    private val STEALTH_CHAR_TO_INDEX_MAP =
        STEALTH_ALPHABET.withIndex().associate { (index, char) -> char to index }



    /**
     * 生成一个符合 AES-256 要求的随机密钥。
     *
     * @return 一个 SecretKey 对象，包含了256位的密钥数据。
     */
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_SIZE_BITS)
        return keyGenerator.generateKey()
    }

    /**
     * 加密一个消息，使用给定的密钥，返回的直接是隐写字符串
     */
    fun encrypt(message: String, key: String): String {
        val plaintextBytes = message.toByteArray(Charsets.UTF_8)
        val encryptedBytes = encryptBytes(plaintextBytes, key)
        return baseNEncode(encryptedBytes)
    }
    // 提供一个重载
    fun encrypt(data: ByteArray, key: String): ByteArray {
        return encryptBytes(data, key)
    }

    //消息解密，智能地从含密文的混合字符串中解密
    fun decrypt(stealthCiphertext: String, key: String): String? {
        val combinedBytes = baseNDecode(stealthCiphertext)
        val decryptedBytes = decryptBytes(combinedBytes, key)
        return decryptedBytes?.toString(Charsets.UTF_8)
    }

    fun decrypt(data: ByteArray, key: String): ByteArray? {
        return decryptBytes(data, key)
    }

    /**
     * ✨ [私有核心] 真正执行加密操作的函数
     */
    private fun encryptBytes(plaintextBytes: ByteArray, key: String): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)    //填充随机内容
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKeyFromString(key), parameterSpec)
        val ciphertextBytes = cipher.doFinal(plaintextBytes)
        // 返回拼接了IV和密文的完整数据
        return iv + ciphertextBytes
    }

    /**
     * ✨ [私有核心] 真正执行解密操作的函数
     */
    private fun decryptBytes(combinedBytes: ByteArray, key: String): ByteArray? {
        try {
            if (combinedBytes.size < IV_LENGTH_BYTES) return null

            val iv = combinedBytes.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertextBytes = combinedBytes.copyOfRange(IV_LENGTH_BYTES, combinedBytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, deriveKeyFromString(key), parameterSpec)

            return cipher.doFinal(ciphertextBytes)
        } catch (e: AEADBadTagException) {
            println("解密失败：数据认证失败，可能已被篡改或密钥错误。\n" + e.message)
            return null
        } catch (e: Exception) {
            println("解密时发生未知错误: ${e.message}")
            return null
        }
    }

    /**
     * 判断给定字符串是否包含密文
     */
    fun containsCiphertext(input: String): Boolean {
        return input.any { STEALTH_CHAR_TO_INDEX_MAP.containsKey(it) }
    }

    fun deriveKeyFromString(keyString: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(keyString.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    // -----------------关键的baseN方法---------------------

    /**
     * 将字节数组编码为我们自定义的 BaseN 字符串。
     * 算法核心：通过大数运算，将 Base256 的数据转换为 BaseN。
     * @param data 原始二进制数据。
     * @return 编码后的“猫语”字符串。
     */
    private fun baseNEncode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        // 使用 BigInteger 来处理任意长度的二进制数据，避免溢出。
        // 构造函数 `BigInteger(1, data)` 确保数字被解释为正数。
        var bigInt = BigInteger(1, data)
        val base = BigInteger.valueOf(STEALTH_ALPHABET.length.toLong())
        val builder = StringBuilder()
        while (bigInt > BigInteger.ZERO) {
            // 除基取余法
            val (quotient, remainder) = bigInt.divideAndRemainder(base)
            bigInt = quotient
            builder.append(STEALTH_ALPHABET[remainder.toInt()])
        }
        // 因为是从低位开始添加的，所以需要反转得到正确的顺序
        return builder.reverse().toString()
    }

    /**
     * 将我们自定义的 BaseN 字符串解码回字节数组。
     * 算法核心：通过大数运算，将 BaseN 的数据转换回 Base256。
     * @param encodedString 编码后的“猫语”字符串，可能混杂有其他字符。
     * @return 原始二进制数据。
     */
    private fun baseNDecode(encodedString: String): ByteArray {
        var bigInt = BigInteger.ZERO
        val base = BigInteger.valueOf(STEALTH_ALPHABET.length.toLong())
        // 遍历字符串，只处理在“猫语字典”中存在的字符
        // 乘基加权法。
        encodedString.forEach { char ->
            val index = STEALTH_CHAR_TO_INDEX_MAP[char]
            if (index != null) {
                // 核心算法: result = result * base + index
                bigInt = bigInt.multiply(base).add(BigInteger.valueOf(index.toLong()))
            }
        }
        // 如果解码结果为0，直接返回空数组
        if (bigInt == BigInteger.ZERO) return ByteArray(0)

        // BigInteger.toByteArray() 可能会在开头添加一个0字节来表示正数，我们需要去掉它
        val bytes = bigInt.toByteArray()
        return if (bytes[0].toInt() == 0) {
            bytes.copyOfRange(1, bytes.size)
        } else { bytes }
    }

    // -- 通过inputStream和outputStream来流式解密 --
    /**
     * 为 AES/GCM 实现的、真正安全的流式解密方法
     * 它会从输入流中读取加密数据，解密后写入输出流。
     * @param inputStream 包含加密数据的输入流 (必须是已经跳过GIF头的数据)
     * @param outputStream 用于写入解密后数据的输出流
     * @param key 用于解密的密钥
     */
    fun decryptStream(inputStream: InputStream, outputStream: OutputStream, key: String){
        val iv = ByteArray(IV_LENGTH_BYTES)
        require(inputStream.read(iv) == IV_LENGTH_BYTES) {
            "输入流太短，无法读取IV。"
        }

        // 2. 初始化 Cipher
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            init(Cipher.DECRYPT_MODE, deriveKeyFromString(key), spec)
        }

        // 3. 边读边解密边写
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            cipher.update(buffer, 0, read)?.let { outputStream.write(it) }
        }

        // 4. 关键！在所有数据都处理完后，调用 doFinal 来验证“防伪标签”
        try {
            cipher.doFinal()?.let { outputStream.write(it) } // 验证通过后，就doFinal做检验，校验不过抛出错误。
        } catch (e: AEADBadTagException) {
            throw SecurityException("解密失败，数据可能被篡改或密钥错误", e)
        }
    }

    /**
     * 为字符串追加一段结构化、有规则的可爱“猫咪话语”后缀。
     *
     * 生成规则：
     * 1. 必须包含固定数量的【猫咪叫声】。
     * 2. 随机包含 0-2 个不重复的【内心活动】，并保证它们不会出现在开头，且被【猫咪叫声】隔开。
     * 3. 随机决定是否在末尾追加一个【颜文字】。
     *
     * @receiver 调用此函数的原始字符串。
     * @return 附加了猫咪话语的新字符串。
     */
    fun String.appendNekoTalk(): String {
        // --- 1. 决定本次生成的组件数量 ---
        val soundCount = 2 // 写死包含2个叫声
        val thoughtCount = Random.nextInt(0, 3) // 随机包含 0, 1, 或 2 个内心活动

        // --- 2. 从库中随机挑选出本次要使用的具体短语 ---
        val soundsToUse = (1..soundCount).map { NEKO_SOUNDS.random() }.toMutableList()
        val thoughtsToUse = NEKO_INNER_THOUGHTS.shuffled().take(thoughtCount)


        // --- 3. 核心逻辑：将【内心活动】插入到【叫声】的间隙中 ---
        // 【关键修改】为了避免内心活动出现在开头，插入点从 1 开始 (即第一个叫声之后)。
        // 这样就保证了第一个元素永远是“叫声”。
        val availableSlots = (1..soundsToUse.size).toMutableList()
        availableSlots.shuffle()

        thoughtsToUse.forEach { thought ->
            if (availableSlots.isNotEmpty()) {
                val insertionIndex = availableSlots.removeAt(0)
                soundsToUse.add(insertionIndex, thought)
            }
        }
        val middleParts = soundsToUse


        // --- 4. 构建最终的后缀字符串 ---
        val fullNekoTalk = buildString {
            middleParts.forEach { part ->
                append(part)
            }

            // 【关键修改】随机决定是否在末尾追加颜文字 (这里设置为 60% 概率)。
            if (Random.nextInt(1, 11) <= 6) {
                val kaomojiToEnd = NEKO_KAOMOJI.random()
                append(kaomojiToEnd)
            }
        }

        val middleIndex = fullNekoTalk.length / 2
        return fullNekoTalk.substring(0, middleIndex) + this + fullNekoTalk.substring(middleIndex)
    }

//    styles: {
//        Bangboo: {//邦布语
//            length: [2, 5],
//            content: ['嗯呢...', '哇哒！', '嗯呢！', '嗯呢哒！', '嗯呐呐！', '嗯哒！', '嗯呢呢！']
//        },
//        Hilichurl: {//丘丘语
//            length: [2, 5],
//            content: ['Muhe ye!', 'Ye dada!', 'Ya yika!', 'Biat ye！', 'Dala si？', 'Yaya ika！', 'Mi? Dada!', 'ye pupu!', 'gusha dada!']
//        },
//        Nier: {//Nier: AutoMata，尼尔语, is that the price I'm paying for my past mistakes?
//            length: [5, 8],
//            content: [
//            "Ee ", "ser ", "les ", "hii ", "san ", "mia ", "ni ", "Escalei ", "lu ", "push ", "to ", "lei ",
//            "Schmosh ", "juna ", "wu ", "ria ", "e ", "je ", "cho ", "no ",
//            "Nasico ", "whosh ", "pier ", "wa ", "nei ", "Wananba ", "he ", "na ", "qua ", "lei ",
//            "Sila ", "schmer ", "ya ", "pi ", "pa ", "lu ", "Un ", "schen ", "ta ", "tii ", "pia ", "pa ", "ke ", "lo "
//            ]
//        },
//        Neko: {//猫娘语
//            length: [3, 5],
//            content: ["嗷呜!", "咕噜~", "喵~", "喵咕~", "喵喵~", "喵?", "喵喵！", "哈！", "喵呜...", "咪咪喵！", "咕咪?"]
//        },
//        Doggo: { // 小狗语
//            length: [2, 5],
//            content: ["汪汪！", "汪呜~", "嗷呜~", "呜汪？", "汪汪呜！", "汪呜呜~", "嗷嗷！"]
//        },
//
//        Birdie: { // 小鸟语
//            length: [2, 5],
//            content: ["啾啾~", "咕咕！", "叽叽~", "啾啾啾！", "叽咕？", "啾啾？", "咕啾~"]
//        },
//
//    },
}

enum class CiphertextStyleType(val displayNameResId:Int,content:List<String>){

}