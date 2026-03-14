package com.skybound.space.core.monitoring

object MonitoringNames {

    object Events {
        const val receiptCaptureStart = "receipt_capture_start"
        const val receiptCaptureSuccess = "receipt_capture_success"
        const val receiptCaptureFail = "receipt_capture_fail"
        const val receiptUploadStart = "receipt_upload_start"
        const val receiptUploadSuccess = "receipt_upload_success"
        const val receiptUploadFail = "receipt_upload_fail"
        const val receiptListLoadSuccess = "receipt_list_load_success"
        const val receiptListLoadFail = "receipt_list_load_fail"
        const val exportStart = "export_start"
        const val exportSuccess = "export_success"
        const val exportFail = "export_fail"
    }

    object Params {
        const val source = "source"
        const val reason = "reason"
        const val selectedCount = "selected_count"
        const val page = "page"
        const val reset = "reset"
        const val resultCount = "result_count"
    }

    object Traces {
        const val homeFirstScreen = "home_first_screen"
        const val receiptUploadPipeline = "receipt_upload_pipeline"
        const val receiptExportPipeline = "receipt_export_pipeline"
        const val receiptListRefresh = "receipt_list_refresh"
        const val uploadPutObjectStorage = "upload_put_object_storage"
    }

    object CrashMetadata {
        const val logLevel = "log_level"
        const val logTag = "log_tag"
        const val logMessage = "log_message"
    }
}
