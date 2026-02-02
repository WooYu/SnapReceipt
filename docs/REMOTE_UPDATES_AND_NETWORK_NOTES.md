# 远程 4 个更新检查 + 网络请求疑问点

## 已实施的优化（本轮）

- **网络模块**：PolicyRepositoryImpl、FileRepositoryImpl、AuthRepositoryImpl 改为使用 foundation 的 `getOrThrow()` / `error.toApiException()`，删除各处私有 `toApiException()`。
- **ApiService**：改为使用 `safeApiCall(...).getOrThrow()`，删除私有 `toApiException()`，并补充 KDoc 说明与 BaseRemoteDataSource 的分工。
- **InvoiceCategoryBottomSheet**：使用 ViewBinding（`BottomSheetInvoiceCategoryBinding`）+ 注入 `CoroutineDispatchersProvider`，协程用 `dispatchers.io` 替代写死的 `Dispatchers.IO`。
- **PolicyWebViewActivity**：为 `EXTRA_TITLE` 补充注释，说明保留供调用方兼容、Custom Tabs 不展示。
- **TokenRefreshAuthenticator**：为 refresh 专用 OkHttpClient 设置较短超时（connect 5s、read 15s、write 10s），避免刷新接口慢时长时间阻塞。
- **远程 2dc1962（协议弹窗）**：`BaseLoginFragment.showAgreementDialog` 在 `show()` 前对 `window` 调用 `setLayout(MATCH_PARENT, WRAP_CONTENT)`，避免部分机型在 `show()` 后 setLayout 不生效。

---

## 一、远程 4 个更新是否还需要优化

### 1. a5ee6a7 - 提交 InvoiceCategoryBottomSheet（已实施）

| 点 | 现状 | 建议 / 实施 |
|----|------|-------------|
| 视图绑定 | ~~`findViewById`~~ | **已改为** ViewBinding（`BottomSheetInvoiceCategoryBinding`） |
| 协程调度 | ~~`Dispatchers.IO` 写死~~ | **已改为** 注入 `CoroutineDispatchersProvider`，使用 `dispatchers.io` |
| 列表刷新 | `notifyDataSetChanged()` | 数据量不大时可接受；若分类很多可考虑 DiffUtil（可选） |
| 分类缓存 | `ReceiptCategory.update(list)` | 无并发问题，保持 |

---

### 2. 2dc1962 - Fix agreement dialog title and width（已实施）

| 点 | 现状 | 建议 / 实施 |
|----|------|-------------|
| 弹窗宽高 | ~~`dialog.show()` 后 `setLayout`~~ | **已改为** 在 `show()` 前对 `dialog.window` 调用 `setLayout(MATCH_PARENT, WRAP_CONTENT)`，兼容更多机型 |
| 其它 | 字符串与布局小改动 | 无明显优化点 |

---

### 3. d498e9b - Cache policy locally and fallback on failure（已实施）

| 点 | 现状 | 建议 / 实施 |
|----|------|-------------|
| 错误转换 | ~~私有 `toApiException()`~~ | **已改为** 使用 foundation 的 `result.error.toApiException()` |
| 本地缓存 | 无 TTL | 若需求不要求“过期时间”可保持 |

---

### 4. 1436c89 - Use Custom Tabs for policies and fix agreement interactions（已实施）

| 点 | 现状 | 建议 / 实施 |
|----|------|-------------|
| Custom Tabs | 打开 URL 后 `finish()` | 符合预期，无需改 |
| EXTRA_TITLE | ~~未使用~~ | **已补充** KDoc 注释：保留供调用方兼容，Custom Tabs 不展示 |

---

## 二、网络请求这块的疑问点

### 1. toApiException / getOrThrow 重复与统一

- **foundation**：`NetworkResult.kt` 已提供 `getOrThrow()` 和 `NetworkError.toApiException()`。
- **仍存在私有 toApiException 的位置**：
  - `core-foundation`：`ApiService.kt`（私有扩展）
  - `core-data`：`PolicyRepositoryImpl`、`FileRepositoryImpl`、`AuthRepositoryImpl`（私有扩展）
- **建议**：上述位置改为使用 foundation 的 `result.getOrThrow()` 或 `error.toApiException()`，删除各处的私有 `toApiException()`，避免逻辑分散与不一致。

---

### 2. ApiService 与 BaseRemoteDataSource 分工

- **ApiService**（foundation）：提供 `request()` / `requestUnit()`，内部 `safeApiCall` 后失败时 `throw result.toApiException()`，返回的是“成功时的 data”或抛异常。
- **BaseRemoteDataSource**（data）：提供 `request()` / `requestUnit()` / `requestEnvelope()`，直接返回 `NetworkResult<T>`，由 Repository 再处理。
- **现状**：data 层全部用 `BaseRemoteDataSource` + Repository 内 `getOrThrow()` 或 `when (result) { ... }`，**未使用 ApiService**。
- **疑问**：ApiService 是否仍计划被使用（例如某层希望“直接拿 T 或抛异常”）？若否，可考虑删除或标注为遗留，避免与 BaseRemoteDataSource 职责混淆。

---

### 3. Token 刷新（TokenRefreshAuthenticator）

- **并发**：`synchronized(refreshLock)` 串行化刷新，避免多请求同时刷新，逻辑合理。
- **阻塞**：`refreshClient.newCall(request).execute()` 为同步调用，刷新期间会阻塞 OkHttp 的调用线程；若刷新接口慢，可能拖慢其它请求。可考虑：
  - 为 refresh 单独设置较短超时，或
  - 评估是否改为异步刷新 + 等待（实现复杂度更高）。
- **失败行为**：返回 `null` 时 OkHttp 不再重试，上层收到 401；`SessionManager.refreshTokenInvalid()` 会发 `RequireLogin`。需确认：登录页是否在收到该事件后立即跳转并清理返回栈，避免用户停留在需登录页。

---

### 4. 超时与重试

- **NetworkConfig**：有 `retryCount` 字段，但当前 OkHttp/Retrofit 构建中**未使用**，实际无自动重试。
- **导出接口**：`ExportTimeoutInterceptor` 对 `/api/receipt/export` 单独延长 read/write 超时（如 60s），其它接口用统一 `readTimeoutSec`。
- **疑问**：是否需要对部分接口（如上传、导出）单独配置更长超时或重试？若需要，可在 NetworkConfig/拦截器里按 path 或 tag 区分。

---

### 5. 日志与安全

- **LoggingInterceptor**：仅在 `config.enableLogging == true` 时添加，一般由 `AppConfig.isDebug` 决定。
- **建议**：确认 release 构建中 `AppConfig.isDebug = false`，避免生产环境打印请求/响应体；若日志包含 token 等敏感信息，需确保不写入持久化日志。

---

### 6. 错误码与 UI 文案

- **ApiException**：仅有 `code`、`message`、`cause`。
- **疑问**：后端若返回业务 code（如 10001 已注销、10002 被封禁），是否需要在 UI 层或统一错误处理（如 BaseViewModel.handleError）里根据 code 做**文案映射**？若目前仅用 `message` 或通用“操作失败”，可后续按产品需求补充 code → 文案映射。

---

### 7. Repository 返回风格：NetworkResult vs getOrThrow

- **当前混用**：
  - 部分 Repository（如 `ReceiptRemoteRepositoryImpl`）统一用 `getOrThrow()`，UseCase 用 `runCatching { repo.xxx() }` 转 `Result`。
  - 部分 Repository（如 `PolicyRepositoryImpl`）内部 `when (result) { is Success -> ... is Failure -> ... }`，对外仍抛异常或返回实体。
- **建议**：约定一种风格并统一，例如：
  - **方案 A**：Repository 一律返回 `NetworkResult<T>` 或直接抛 `ApiException`，UseCase 内统一 `runCatching` 转 `Result<T>`；
  - **方案 B**：Repository 统一用 `getOrThrow()`，UseCase 统一 `runCatching`。
  统一后便于错误处理与单测写法一致。

---

### 8. 其它

- **AuthInterceptor**：通过 `No-Auth` 头跳过注入 Authorization，逻辑清晰；需保证仅对“明确不需要登录”的接口（如登录、拉取协议）加该头，避免误用。
- **ExportTimeoutInterceptor**：`exportPath` 写死为 `/api/receipt/export`，若后端 path 变更需同步改；若将来有多个“长超时”接口，可考虑配置化 path 或超时时间。

---

以上为对“远程 4 个更新”的优化建议与“网络请求”相关疑问/改进点的整理，可按优先级分批落地（先统一 toApiException/getOrThrow 与 Repository 风格，再视需求做超时/重试/错误码映射）。
