# Android MVVM 架构从0到1搭建核对文档（可下载版）

**文档说明**

1. 核对目标：验证从零搭建的 Android MVVM 架构是否符合「分层解耦、可维护、可扩展、可测试」的核心要求，确保所有搭建步骤无遗漏、规范无偏差。

2. 核对范围：项目环境配置、四层架构落地、核心原则合规、目录命名规范。

3. 使用方式：按模块逐一核对，标记「是否达标」，对未达标项需按「核对标准」整改，核心项（标注★）必须 100% 达标。

## 一、 项目环境与基础配置核对

|序号|检查内容|核对标准|是否达标|备注|
|---|---|---|---|---|
|1|项目初始化|★ 选择 Kotlin 作为开发语言；★ 最小 SDK 不低于 21； 无多余默认冗余代码（如默认 MainActivity 无关逻辑）|||
|2|核心依赖配置|★ 引入 Jetpack 核心组件（ViewModel、Lifecycle）；★ 引入协程/Flow（kotlinx-coroutines-android）；★ 引入依赖注入框架（Hilt）；★ 引入网络（Retrofit+OkHttp）、本地存储（Room）、日志（Timber）；★ 使用 Gradle Version Catalog 统一管理依赖版本，无版本冲突|||
|3|编译选项配置|★ 启用 Kotlin 协程支持；★ 配置 Hilt 注解处理器；★ 可选：启用 ViewBindingCompose 相关配置；★ 启用 Java 8 及以上兼容编译|||
## 二、 四层架构搭建核对（核心模块）

### 2.1 基础层（Core）核对

|序号|检查内容|核对标准|是否达标|备注|||||
|---|---|---|---|---|---|---|---|---|
|1|全局 Application 类|★ 添加 `@HiltAndroidApp` 注解；★ 在 AndroidManifest.xml 中注册；★ 初始化全局工具（Timber、第三方库等），无业务相关代码|||||||
|2|依赖注入模块配置|★ 拆分 NetworkModule、DatabaseModule、AppModule；★ 提供 OkHttp、Retrofit、Room 等全局单例实例；★ 注解使用规范（@Module、@InstallIn、@Provides、@Singleton）；★ 无硬编码（如 BASE_URL 放入常量类）|||||||
|3|全局能力封装|★ 定义完整业务异常体系（网络、接口、本地数据异常）；★ 封装通用工具类（日期、字符串、权限等），无 Context 硬依赖；★ 统一日志输出（Timber），禁止使用 Log 类；★ 封装全局常量类，统一管理公共配置|||||||
|4|层边界约束|★ 无任何业务相关代码；★ 仅提供全局支撑能力，不与上层业务耦合||序号|检查内容|核对标准|是否达标|备注|
|1|领域实体（Entity）|★ 仅包含业务核心字段，无 UI/数据传输冗余字段；★ 采用不可变设计（data class，无可变字段）；★ 无 Android 框架依赖（如 Context、Bitmap）；★ 命名规范：XXXEntity|||||||
|2|Repository 接口|★ 定义数据操作契约，覆盖所有业务数据场景；★ 方法支持协程（suspend）或流式数据（Flow）；★ 返回值为领域 Entity 或标准化结果类型；★ 不感知数据来源（本地/远程），无具体实现逻辑；★ 命名规范：XXXRepository|||||||
|3|UseCase 封装|★ 单一 UseCase 对应单一业务场景，符合单一职责；★ 构造函数注入 Repository 接口，无硬编码实例化；★ 封装业务逻辑，处理异常并返回标准化结果；★ 无 Android 框架依赖，无 UI 相关逻辑；★ 命名规范：XXXUseCase|||||||
|4|层边界约束|★ 独立于 Android 框架，可脱离 Android 环境单元测试；★ 仅依赖自身定义的接口，不依赖上层下层实现类|||||||
### 2.3 数据层（Data）核对

|序号|检查内容|核对标准|是否达标|备注|
|---|---|---|---|---|
|1|DTO 模型定义|★ 对应网络接口/本地数据库结构，序列化注解完整；★ 提供 toEntity()toDto() 转换方法，实现与领域 Entity 的互转；★ 命名规范：XXXDto|||
|2|数据源封装|★ 拆分远程数据源（XXXRemoteDataSource）和本地数据源（XXXLocalDataSource）；★ 仅负责数据读写，无业务逻辑；★ 处理底层异常（网络超时、数据库操作失败），转换为业务异常；★ 方法返回值为 DTO 类型，支持协程异步操作|||
|3|Repository 实现类|★ 实现领域层定义的 Repository 接口，覆盖所有方法；★ 注入远程本地数据源，协调数据优先级（如本地优先）；★ 完成 DTO 与 Entity 的转换，返回领域层要求的类型；★ 处理数据同步逻辑（如远程数据获取后同步到本地）；★ 命名规范：XXXRepositoryImpl|||
|4|层边界约束|★ 仅依赖领域层接口和自身实现，不依赖上层（ViewModel/UI）；★ 不暴露数据源细节给上层，仅通过 Repository 接口对外提供能力|||
### 2.4 表现层（View + ViewModel）核对

|序号|检查内容|核对标准|是否达标|备注|
|---|---|---|---|---|
|1|UI 状态定义|★ 每个页面对应 XXXUiState，封装全量状态（加载中、成功、失败、空数据）；★ 状态不可变，通过副本更新（密封类或 data class + copy 方法）；★ 包含业务数据（Entity 类型）和用户友好提示信息|||
|2|ViewModel 开发|★ 添加 `@HiltViewModel` 注解，支持 Hilt 注入；★ 构造函数注入 UseCase，无硬编码实例化；★ 私有 MutableStateFlow 持有 UI 状态，公开不可变 StateFlow 供 UI 订阅；★ 使用 viewModelScope 启动协程，自动跟随生命周期取消；★ 封装业务入口方法，转发用户操作，更新 UI 状态；★ 无 ViewContext 引用，无 UI 渲染逻辑；★ 命名规范：XXXViewModel|||
|3|UI 页面开发（Activity/Fragment）|★ 添加 `@AndroidEntryPoint` 注解，支持 ViewModel 注入；★ 通过 `by viewModels()` 获取 ViewModel 实例；★ 仅负责 UI 初始化和渲染，无业务逻辑、无数据读写；★ 生命周期安全订阅 StateFlow（repeatOnLifecycle 作用域）；★ 用户操作（按钮点击等）转发给 ViewModel 公开方法；★ 命名规范：XXXActivityXXXFragment|||
|4|层边界约束|★ View 层仅与 ViewModel 交互，不直接访问 UseCase/Repository；★ ViewModel 仅与 UseCase 交互，不直接访问数据层实现类|||
## 三、 架构核心原则合规性核对

|序号|核心原则|检查内容|核对标准|是否达标|备注|
|---|---|---|---|---|---|
|1|关注点分离|各层职责边界|★ UI 层：仅渲染+交互，无业务/数据逻辑；★ ViewModel 层：仅状态管理+业务转发，无 UI数据实现逻辑；★ 领域层：仅业务逻辑，无 Android 依赖/数据细节；★ 数据层：仅数据读写，无业务逻辑；★ 基础层：仅全局支撑，无业务相关代码|||
|2|依赖倒置|层间依赖关系|★ 上层依赖下层接口，不依赖下层实现类；★ 所有实例化通过依赖注入实现，禁止 new 关键字硬编码；★ 下层不依赖上层，无反向引用|||
|3|单一职责|类/模块功能|★ 每个类仅负责一项功能（如 UseCase 仅封装一个业务场景）；★ 无全能类（如既处理网络又处理 UI 的工具类）|||
|4|数据不可变|状态/实体数据|★ UI 状态、领域 Entity 无可变字段；★ 状态更新通过生成新副本实现，不直接修改原数据；★ 无并发状态异常风险|||
|5|流式数据传递|数据/状态分发|★ 全流程使用 StateFlow/SharedFlow 实现响应式数据传递；★ 禁止使用接口回调、EventBus 等传统通信方式；★ 状态变化自动通知上层，无需手动刷新|||
## 四、 工程目录与命名规范核对

|序号|检查内容|核对标准|是否达标|备注|
|---|---|---|---|---|
|1|目录结构|★ 按四层架构分层目录，结构清晰：app → src → main → java → 包名 → ├── core（基础层）├── domain（领域层）├── data（数据层）├── ui（表现层）|||
|2|命名规范|★ 实体类：XXXEntity；★ Repository 接口：XXXRepository；★ Repository 实现类：XXXRepositoryImpl；★ UseCase：XXXUseCase；★ DTO：XXXDto；★ 数据源：XXXRemoteDataSourceXXXLocalDataSource；★ ViewModel：XXXViewModel；★ UI 状态：XXXUiState；★ 页面：XXXActivityXXXFragment|||
## 五、 最终验收总结

1. 必须达标项（★）：所有标注核心项需全部达标，确保架构的核心稳定性、可维护性。

2. 优化项：非核心项可根据项目规模灵活调整（如简单项目可省略部分 UseCase 封装），但需保留架构扩展能力。

3. 验收通过标准：核心项 100% 达标，非核心项≥90% 达标，目录结构清晰，命名规范统一，无跨层耦合、无内存泄漏风险。

4. 后续扩展建议：
        

    - 预留组件化拆分接口，支持多模块开发；

    - 补充单元测试/UI 测试，验证各层逻辑正确性；

    - 强化离线缓存策略，提升无网络环境可用性。

### 核对完成备注

需将未达标项整改完成后，再进行项目后续开发，确保架构基础扎实，避免后续重构成本。
> （注：文档部分内容可能由 AI 生成）