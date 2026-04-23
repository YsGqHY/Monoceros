<div align="center">
  <img src=".assets/banner.png" alt="Monoceros" width="600" />
</div>

# Monoceros

[![License: MIT](https://img.shields.io/github/license/YsGqHY/Monoceros?style=flat-square)](LICENSE)
[![Version](https://img.shields.io/github/v/release/YsGqHY/Monoceros?style=flat-square&label=version&color=blue)](https://github.com/YsGqHY/Monoceros/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2--26.1.2-green?style=flat-square)](https://www.minecraft.net)
[![TabooLib](https://img.shields.io/badge/TabooLib-6.3.0-orange?style=flat-square)](https://github.com/TabooLib/TabooLib)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Fluxon](https://img.shields.io/badge/Fluxon-1.6.24-blueviolet?style=flat-square)](https://github.com/TabooLib/Fluxon)

基于 TabooLib 的跨版本 Minecraft 服务器机制与玩法平台。

Monoceros 不是一个单点功能插件，而是面向服务器玩法开发的 **监听 + 分发 + 脚本 + 工作流 + 跨版本适配** 基础设施。

## 核心能力

| 系统 | 说明 |
|---|---|
| **事件分发** | 统一的 Bukkit 事件路由体系，支持优先级、权重排序、动态注册/注销与热重载 |
| **脚本引擎** | 以 Fluxon 为核心运行时，支持编译缓存、预热、增量重载与任务跟踪 |
| **调度系统** | 延迟、周期、Cron 与条件任务，配置驱动 |
| **命令系统** | 命令树、参数约束、Tab 补全、脚本命令入口 |
| **数据包监听** | Packet 收发、过滤、匹配、追踪、拦截与覆写（Wireshark） |
| **挥发能力** | 伪方块、世界边界、实体元数据、客户端幻象等 NMS 能力（Volatility） |
| **动作工作流** | 实体、事件、幻象、物品、记忆、目标六大动作域 |
| **属性工作流** | 通用属性与实体属性的上下文访问与写回 |
| **机制服务** | 战斗、区域、交互、视觉、会话五大玩法机制 |
| **跨版本适配** | 覆盖 1.12.2 ~ 26.1.2，版本差异通过独立模块隔离 |

## 项目结构

```
plugin/                          最终聚合打包
project/
├── common/                      API 与抽象接口
├── common-impl/                 默认实现与 Fluxon 运行时
├── module-bukkit/               Bukkit 平台入口
├── module-command/              命令系统
├── module-dispatcher/           事件分发
├── module-script/               脚本加载与热重载
├── module-schedule/             调度系统
├── module-volatility/           NMS 挥发能力
├── module-wireshark/            数据包监听
├── workflow-action/             动作工作流核心
├── workflow-property/           属性工作流核心
├── workflow-resources/          默认配置与模板
├── extension-action-*/          动作扩展（实体/事件/幻象/物品/记忆/目标）
├── extension-property-*/        属性扩展（通用/实体）
├── module-legacy-api/           旧版本兼容层
├── module-modern/               现代版本能力层
├── module-java17/               Java 17 专属模块
└── module-java21/               Java 21 专属模块
```

## 配套 Skill

本项目提供 Agent Skill，用于辅助配置编写与开发：

| Skill | 说明 |
|---|---|
| **Monoceros** | 插件配置编写知识库，涵盖分发器、调度、工作流、数据包、命令、机制服务、挥发能力与版本适配 |


## 构建

构建发行版本（不含 TabooLib 本体）：

```bash
./gradlew build
```

构建开发版本（含 TabooLib 本体，不可运行）：

```bash
./gradlew taboolibBuildApi -PDeleteCode
```

## 许可证

[MIT License](LICENSE)
