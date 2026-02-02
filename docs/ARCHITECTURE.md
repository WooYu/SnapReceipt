# SnapReceipt 架构说明

## 包名与模块

- **`com.skybound.space`**（core-base、core-foundation）：通用基础库，提供 UI 基类、网络、DI、配置、工具等，与具体业务解耦。
- **`com.snapreceipt.io`**（app、core-data、core-domain）：SnapReceipt 业务代码，包括收据管理、登录、个人中心等。

约定：基础库保持 `skybound.space`，业务层使用 `snapreceipt.io`，便于区分“可复用基础能力”与“收据业务”。

## 导航

主界面采用 **手动 Tab + FragmentManager.replace**（未使用 Jetpack Navigation 的 NavController），结构简单、无深层链接需求；若后续需要 Deep Link、统一返回栈或 Safe Args，可再迁移到 Navigation Component。

## 数据层

- **BaseRepository**：仅本地或混合（本地+远程）仓库继承，复用 `withIo`、`mapFlow`、`cacheFirst`；纯远程仓库可不继承。详见 [BaseRepository](core-data/src/main/java/com/snapreceipt/io/data/base/BaseRepository.kt) 注释。
