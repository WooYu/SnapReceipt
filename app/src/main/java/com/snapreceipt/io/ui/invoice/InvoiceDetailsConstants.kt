package com.snapreceipt.io.ui.invoice

/**
 * 发票详情 Activity 使用的常量
 * 分类：数据传递、操作类型、来源场景
 */
object InvoiceDetailsConstants {
    // ==================== Intent Extras ====================
    const val EXTRA_START_TAB = "extra_start_tab"
    const val EXTRA_SOURCE_SCENE = "extra_source_scene"
    const val EXTRA_OPERATION_TYPE = "extra_operation_type"
    const val EXTRA_RECEIPT = "extra_receipt"
    const val EXTRA_RECEIPT_ID = "extra_receipt_id"

    // ==================== Tab Types ====================
    const val TAB_RECEIPTS = "receipts"

    // ==================== Source Scene ====================
    /** 从扫描功能跳转，直接进入编辑状态 */
    const val SOURCE_SCAN = "scan_source"
    
    /** 从首页列表跳转，已保存收据 */
    const val SOURCE_RECEIPTS_LIST = "receipts_list"
    
    /** 从发票列表跳转，已保存收据 */
    const val SOURCE_INVOICES_LIST = "invoices_list"

    // ==================== Operation Types ====================
    /** 新增收据操作 */
    const val OPERATION_TYPE_ADD = "operation_add"
    
    /** 编辑收据操作 */
    const val OPERATION_TYPE_UPDATE = "operation_update"
    
    /** 删除收据操作 */
    const val OPERATION_TYPE_DELETE = "operation_delete"
}
