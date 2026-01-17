/**
 * 字符串资源配置系统
 * 
 * 设计特点:
 * - 集中管理所有字符串常量
 * - 支持多语言国际化
 * - 支持运行时动态切换语言
 * - 支持字符串参数化
 */
package com.snapreceipt.io.config.strings

import java.util.*

/**
 * 字符串资源接口
 */
interface StringResources {
    // 通用字符串
    fun appName(): String
    fun appVersion(): String
    
    // 导航和UI
    fun homeTitle(): String
    fun settingsTitle(): String
    fun aboutTitle(): String
    fun profileTitle(): String
    
    // 按钮和操作
    fun buttonConfirm(): String
    fun buttonCancel(): String
    fun buttonSave(): String
    fun buttonDelete(): String
    fun buttonEdit(): String
    fun buttonBack(): String
    fun buttonNext(): String
    fun buttonPrevious(): String
    fun buttonRetry(): String
    fun buttonClose(): String
    
    // 提示消息
    fun messageSaveSuccess(): String
    fun messageDeleteSuccess(): String
    fun messageLoadingError(): String
    fun messageNetworkError(): String
    fun messageEmptyData(): String
    fun messageNoPermission(): String
    
    // OCR 相关
    fun ocrTitle(): String
    fun ocrSelectImage(): String
    fun ocrProcessing(): String
    fun ocrSuccess(result: String): String
    fun ocrFailed(): String
    fun ocrNoResult(): String
    
    // 设置相关
    fun settingsTheme(): String
    fun settingsLanguage(): String
    fun settingsNotifications(): String
    fun settingsAbout(): String
    fun settingsVersion(): String
    fun settingsPrivacy(): String
    
    // 错误消息
    fun errorUnknown(): String
    fun errorNetwork(): String
    fun errorTimeout(): String
    fun errorInvalidInput(): String
    fun errorPermissionDenied(): String
    fun errorServerError(): String
    
    // 验证消息
    fun validateEmailRequired(): String
    fun validateEmailInvalid(): String
    fun validatePasswordRequired(): String
    fun validatePasswordTooShort(): String
    fun validateFieldRequired(fieldName: String): String
}

/**
 * 英文字符串资源实现
 */
class EnglishStringResources : StringResources {
    override fun appName() = "SnapReceipt"
    override fun appVersion() = "1.0.0"
    
    override fun homeTitle() = "Home"
    override fun settingsTitle() = "Settings"
    override fun aboutTitle() = "About"
    override fun profileTitle() = "Profile"
    
    override fun buttonConfirm() = "Confirm"
    override fun buttonCancel() = "Cancel"
    override fun buttonSave() = "Save"
    override fun buttonDelete() = "Delete"
    override fun buttonEdit() = "Edit"
    override fun buttonBack() = "Back"
    override fun buttonNext() = "Next"
    override fun buttonPrevious() = "Previous"
    override fun buttonRetry() = "Retry"
    override fun buttonClose() = "Close"
    
    override fun messageSaveSuccess() = "Saved successfully"
    override fun messageDeleteSuccess() = "Deleted successfully"
    override fun messageLoadingError() = "Failed to load data"
    override fun messageNetworkError() = "Network error. Please check your connection."
    override fun messageEmptyData() = "No data available"
    override fun messageNoPermission() = "Permission denied"
    
    override fun ocrTitle() = "Receipt Scanner"
    override fun ocrSelectImage() = "Select Image"
    override fun ocrProcessing() = "Processing..."
    override fun ocrSuccess(result: String) = "Recognized: $result"
    override fun ocrFailed() = "Failed to process image"
    override fun ocrNoResult() = "No text found in image"
    
    override fun settingsTheme() = "Theme"
    override fun settingsLanguage() = "Language"
    override fun settingsNotifications() = "Notifications"
    override fun settingsAbout() = "About"
    override fun settingsVersion() = "Version"
    override fun settingsPrivacy() = "Privacy Policy"
    
    override fun errorUnknown() = "An unknown error occurred"
    override fun errorNetwork() = "Network error"
    override fun errorTimeout() = "Request timeout"
    override fun errorInvalidInput() = "Invalid input"
    override fun errorPermissionDenied() = "Permission denied"
    override fun errorServerError() = "Server error. Please try again later."
    
    override fun validateEmailRequired() = "Email is required"
    override fun validateEmailInvalid() = "Please enter a valid email"
    override fun validatePasswordRequired() = "Password is required"
    override fun validatePasswordTooShort() = "Password must be at least 8 characters"
    override fun validateFieldRequired(fieldName: String) = "$fieldName is required"
}

/**
 * 中文字符串资源实现
 */
class ChineseStringResources : StringResources {
    override fun appName() = "SnapReceipt"
    override fun appVersion() = "1.0.0"
    
    override fun homeTitle() = "首页"
    override fun settingsTitle() = "设置"
    override fun aboutTitle() = "关于"
    override fun profileTitle() = "个人资料"
    
    override fun buttonConfirm() = "确认"
    override fun buttonCancel() = "取消"
    override fun buttonSave() = "保存"
    override fun buttonDelete() = "删除"
    override fun buttonEdit() = "编辑"
    override fun buttonBack() = "返回"
    override fun buttonNext() = "下一步"
    override fun buttonPrevious() = "上一步"
    override fun buttonRetry() = "重试"
    override fun buttonClose() = "关闭"
    
    override fun messageSaveSuccess() = "保存成功"
    override fun messageDeleteSuccess() = "删除成功"
    override fun messageLoadingError() = "加载数据失败"
    override fun messageNetworkError() = "网络错误，请检查您的网络连接"
    override fun messageEmptyData() = "暂无数据"
    override fun messageNoPermission() = "权限被拒绝"
    
    override fun ocrTitle() = "收据扫描"
    override fun ocrSelectImage() = "选择图片"
    override fun ocrProcessing() = "处理中..."
    override fun ocrSuccess(result: String) = "识别结果: $result"
    override fun ocrFailed() = "处理图片失败"
    override fun ocrNoResult() = "图片中未找到文本"
    
    override fun settingsTheme() = "主题"
    override fun settingsLanguage() = "语言"
    override fun settingsNotifications() = "通知"
    override fun settingsAbout() = "关于"
    override fun settingsVersion() = "版本"
    override fun settingsPrivacy() = "隐私政策"
    
    override fun errorUnknown() = "发生未知错误"
    override fun errorNetwork() = "网络错误"
    override fun errorTimeout() = "请求超时"
    override fun errorInvalidInput() = "输入无效"
    override fun errorPermissionDenied() = "权限被拒绝"
    override fun errorServerError() = "服务器错误，请稍后重试"
    
    override fun validateEmailRequired() = "邮箱不能为空"
    override fun validateEmailInvalid() = "请输入有效的邮箱地址"
    override fun validatePasswordRequired() = "密码不能为空"
    override fun validatePasswordTooShort() = "密码至少需要8个字符"
    override fun validateFieldRequired(fieldName: String) = "$fieldName 不能为空"
}

/**
 * 字符串资源管理器
 */
class StringResourceManager {
    private var currentResources: StringResources = EnglishStringResources()
    
    fun getCurrentResources(): StringResources = currentResources
    
    fun setResources(resources: StringResources) {
        currentResources = resources
    }
    
    fun switchToEnglish() {
        currentResources = EnglishStringResources()
    }
    
    fun switchToChinese() {
        currentResources = ChineseStringResources()
    }
    
    fun switchByLocale(locale: Locale) {
        currentResources = when (locale.language) {
            "zh" -> ChineseStringResources()
            else -> EnglishStringResources()
        }
    }
    
    fun getAvailableLanguages(): List<Pair<String, StringResources>> = listOf(
        "English" to EnglishStringResources(),
        "中文" to ChineseStringResources()
    )
}
