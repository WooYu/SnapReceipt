package com.skybound.space.core.monitoring

object MonitoringNames {

    object Events {
        // 用户从相机或相册开始小票采集流程时触发。
        const val receiptCaptureStart = "receipt_capture_start"
        // 小票采集成功并产出可用图片时触发。
        const val receiptCaptureSuccess = "receipt_capture_success"
        // 小票采集失败，未能产出可用图片时触发。
        const val receiptCaptureFail = "receipt_capture_fail"
        // 小票上传流程开始时触发。
        const val receiptUploadStart = "receipt_upload_start"
        // 小票上传流程成功完成时触发。
        const val receiptUploadSuccess = "receipt_upload_success"
        // 小票上传流程任一环节失败时触发。
        const val receiptUploadFail = "receipt_upload_fail"
        // 小票列表请求成功返回时触发。
        const val receiptListLoadSuccess = "receipt_list_load_success"
        // 小票列表请求失败时触发。
        const val receiptListLoadFail = "receipt_list_load_fail"
        // 导出所选小票开始时触发。
        const val exportStart = "export_start"
        // 导出所选小票成功完成时触发。
        const val exportSuccess = "export_success"
        // 导出所选小票失败时触发。
        const val exportFail = "export_fail"
    }

    object Params {
        // 标记动作来源，例如相机或相册。
        const val source = "source"
        // 记录统一归一化后的失败原因，便于统计和排查。
        const val reason = "reason"
        // 记录批量操作中选中的小票数量。
        const val selectedCount = "selected_count"
        // 记录分页列表请求的页码。
        const val page = "page"
        // 标记列表请求是整页刷新还是增量加载。
        const val reset = "reset"
        // 记录一次列表请求返回的小票数量。
        const val resultCount = "result_count"
    }

    object Traces {
        // 衡量首页首屏关键内容渲染耗时。
        const val homeFirstScreen = "home_first_screen"
        // 衡量小票上传整条链路的端到端耗时。
        const val receiptUploadPipeline = "receipt_upload_pipeline"
        // 衡量小票导出整条链路的端到端耗时。
        const val receiptExportPipeline = "receipt_export_pipeline"
        // 衡量小票列表刷新或分页请求耗时。
        const val receiptListRefresh = "receipt_list_refresh"
        // 衡量通过预签名 URL 向对象存储执行 PUT 上传的耗时。
        const val uploadPutObjectStorage = "upload_put_object_storage"
    }

    object CrashMetadata {
        // 随异常一起上报原始日志级别。
        const val logLevel = "log_level"
        // 随异常一起上报原始日志标签。
        const val logTag = "log_tag"
        // 随异常一起上报原始日志内容。
        const val logMessage = "log_message"
    }
}
