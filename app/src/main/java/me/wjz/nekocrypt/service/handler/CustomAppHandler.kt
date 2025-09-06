package me.wjz.nekocrypt.service.handler

import android.view.accessibility.AccessibilityEvent
import com.dianming.phoneapp.MyAccessibilityService
import kotlinx.serialization.Serializable

/**
 * 一个数据类，用于表示用户自定义的应用配置。
 * @Serializable 注解是必须的，它告诉 kotlinx.serialization 库这个类可以被转换成JSON。
 */
@Serializable
data class CustomAppHandler(
    // 用户为这个配置起的名字
    var customName: String,

    // 需要重写 ChatAppHandler 接口中的所有属性
    override val name: String,
    override val packageName: String,
    override val inputId: String,
    override val sendBtnId: String,
    override val messageTextId: String,
    override val messageListClassName: String

) : ChatAppHandler {
    // ChatAppHandler 中的方法可以暂时为空，因为自定义应用的行为会更通用
    override fun onHandlerActivated(service: MyAccessibilityService) {}
    override fun onHandlerDeactivated() {}
    override fun onAccessibilityEvent(
        event: AccessibilityEvent,
        service: MyAccessibilityService
    ) {}
}
