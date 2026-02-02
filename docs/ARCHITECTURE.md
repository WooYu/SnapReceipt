# SnapReceipt 架构说明

## 包名与模块

- **`com.skybound.space`**（core-base、core-foundation）：通用基础库，提供 UI 基类、网络、DI、配置、工具等，与具体业务解耦。
- **`com.snapreceipt.io`**（app、core-data、core-domain）：SnapReceipt 业务代码，包括收据管理、登录、个人中心等。

约定：基础库保持 `skybound.space`，业务层使用 `snapreceipt.io`，便于区分“可复用基础能力”与“收据业务”。

## 导航

主界面已迁移至 **Jetpack Navigation Component**：`activity_main` 使用 `NavHostFragment` + `main_nav` 导航图，底部栏通过 `setupWithNavController(binding.bottomNav, navController)` 与 NavController 联动；支持从外部（如发票详情保存后）通过 `EXTRA_START_TAB` 指定初始 Tab（如收据列表）。

## 数据层

- **BaseRepository**：仅本地或混合（本地+远程）仓库继承，复用 `withIo`、`mapFlow`、`cacheFirst`；纯远程仓库可不继承。详见 [BaseRepository](core-data/src/main/java/com/snapreceipt/io/data/base/BaseRepository.kt) 注释。
