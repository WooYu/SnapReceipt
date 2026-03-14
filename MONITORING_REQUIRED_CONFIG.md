# SnapReceipt Monitoring Required Config

## 1. Firebase Console 必做项

- Firebase 项目中的 Android App 包名必须是 `com.snapreceipt.io`。
- 在同一 Firebase 项目中启用 `Analytics`、`Crashlytics`、`Performance Monitoring`。
- 在 `Project settings > Integrations` 中确认 Firebase 已连接到对应的 Google Play 应用。
- 为 release 构建保留 Crashlytics mapping 上传能力，避免 CI 屏蔽 Crashlytics 相关 task。
- 如需实时验证 Analytics，启用 DebugView。

## 2. Google Play Console Android vitals 看板

- 在 `Android vitals` 建立固定版本对比视图，至少包含：
  - Crash rate
  - ANR rate
  - App startup
  - Slow rendering / frozen frames
- 每次 release 后，固定对比上一正式版本，检查是否出现回归。
- 对高风险版本记录：
  - 版本号
  - 上线日期
  - Crash/ANR 是否超阈值

## 3. 隐私政策 / Data Safety 申明点

- 申明会采集崩溃信息，用于稳定性诊断与问题修复。
- 申明会采集性能信息，用于性能监控与回归分析。
- 申明会采集分析事件，用于产品行为统计与关键路径分析。
- 说明应用内提供监控开关，用户可关闭崩溃上报、分析上报，并可按需开启诊断日志模式。
- 如隐私政策区分自动采集与用户主动分享，需补充“诊断日志仅在本地生成，由用户手动导出分享”。

## 4. `google-services.json` 放置规则

- 文件路径固定为 [`app/google-services.json`](/f:/Code/SnapReceipt/SnapReceipt_ v1.0_Codex/SnapReceipt/app/google-services.json)。
- 该文件必须来自当前 Firebase 项目，且对应包名 `com.snapreceipt.io`。
- 若 Firebase 项目变更，必须重新下载并替换该文件。
- 不要把其他环境或其他包名的 `google-services.json` 复用到当前 app。

## 5. 内测验证清单

- 手工触发一次测试崩溃，确认 Crashlytics 控制台可见新 crash。
- 手动调用 `ExceptionMonitorManager.report(...)`，确认 Crashlytics 可见 non-fatal。
- 在 DebugView 验证 `receipt_capture_*`、`receipt_upload_*`、`receipt_list_load_*`、`export_*` 事件。
- 在 Performance 控制台验证：
  - `screen_load_home_first_screen`
  - `api_call_receipt_upload_pipeline`
  - `api_call_receipt_list_refresh`
  - `api_call_receipt_export_pipeline`
  - `api_call_upload_put_object_storage`
- 在设置页分别关闭：
  - `崩溃上报`
  - `分析上报`
  - `诊断日志模式`
  确认关闭后不再产生对应新数据。
- 在设置页点击 `导出诊断日志`，确认能拉起分享面板并分享 zip。
- 在无网络或 Google 服务不可用的设备上验证 App 不崩溃。

## 6. ADB 本地拉取诊断日志命令

- 查看日志目录：

```bash
adb shell run-as com.snapreceipt.io ls -R files/diagnostics/logs
```

- 直接导出整个诊断日志目录到本机 tar：

```bash
adb exec-out run-as com.snapreceipt.io sh -c "tar -cf - files/diagnostics/logs" > snapreceipt-diagnostics.tar
```

- 直接导出当前日志文件到本机：

```bash
adb exec-out run-as com.snapreceipt.io sh -c "cat files/diagnostics/logs/diagnostic-current.log" > diagnostic-current.log
```

- 查看导出 zip 缓存目录：

```bash
adb shell run-as com.snapreceipt.io ls -R cache/diagnostics
```
