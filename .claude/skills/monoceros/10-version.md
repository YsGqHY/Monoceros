# Monoceros 版本适配参考

## 概述

Monoceros 支持 Minecraft `1.12.2 ~ 26.1.2` 版本范围。版本差异通过接口、工厂、服务注册和独立模块解决，不在业务代码中堆版本判断。

## 版本档案

`VersionProfile` 数据类：

| 字段 | 类型 | 说明 |
|------|------|------|
| `minecraftVersionId` | Int | 版本数字 ID（major*10000 + minor*100 + patch） |
| `javaVersion` | Int | 当前 Java 版本号 |
| `profileId` | String | 档案标识字符串 |
| `legacyMode` | Boolean | 是否为旧版模式（1.12.x 及以下） |
| `modernMode` | Boolean | 是否为现代模式 |

版本 ID 计算示例：

| Minecraft 版本 | versionId | legacyMode |
|----------------|-----------|------------|
| 1.12.2 | 11202 | true |
| 1.16.5 | 11605 | false |
| 1.19.4 | 11904 | false |
| 1.20.4 | 12004 | false |
| 1.21.1 | 12101 | false |

## 特性标志

`FeatureFlags` 数据类，标识当前版本支持的特性：

| 字段 | 类型 | 启用条件 | 说明 |
|------|------|----------|------|
| `legacyNbt` | Boolean | 旧版本 | 使用旧版 NBT 系统 |
| `dataComponent` | Boolean | >= 1.20.5 | 支持 Data Component API |
| `itemModel` | Boolean | 现代版本 | 支持物品模型系统 |
| `packetRewriteSafe` | Boolean | >= 1.19.0 | 数据包覆写安全 |

## 版本适配解析器

`VersionAdapterResolver` 接口：

| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `currentProfile()` | VersionProfile | 获取当前版本档案 |
| `featureFlags()` | FeatureFlags | 获取当前特性标志 |
| `resolveOrNull(type)` | T? | 按类型解析版本适配实现 |

版本信息从 `Bukkit.getBukkitVersion()` 自动解析。

## 版本服务接口

### VersionedService

所有版本相关服务的基接口：

```kotlin
interface VersionedService {
    fun supports(profile: VersionProfile): Boolean
}
```

### VersionModuleProvider

版本模块提供者：

```kotlin
interface VersionModuleProvider : VersionedService {
    val moduleId: String
}
```

## 材质映射服务（MaterialMappingService）

处理跨版本材质名称差异：

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `matchMaterial` | name: String | Material? | 按名称匹配材质 |
| `canonicalName` | material: Material | String | 获取材质规范名称 |

## 文本处理服务（TextProcessingService）

处理跨版本文本颜色差异：

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `colorize` | text: String | String | 颜色代码转换 |
| `stripColor` | text: String | String | 去除颜色代码 |

## 物品桥接服务（ItemMetaBridgeService）

处理跨版本物品元数据差异：

| 方法 | 参数 | 返回 | 说明 |
|------|------|------|------|
| `getDisplayName` | item: ItemStack | String? | 获取物品显示名 |
| `setDisplayName` | item: ItemStack, name: String? | -- | 设置物品显示名 |
| `getLore` | item: ItemStack | List<String> | 获取物品描述 |
| `setLore` | item: ItemStack, lore: List<String> | -- | 设置物品描述 |
| `getCustomModelData` | item: ItemStack | Int | 获取自定义模型数据 |
| `setCustomModelData` | item: ItemStack, data: Int | -- | 设置自定义模型数据 |

## 模块分层

| 模块 | JVM Target | 说明 |
|------|------------|------|
| `module-legacy-api` | 1.8 | 旧版本兼容层（旧 Material、旧文本、旧 NBT） |
| `module-modern` | 1.8 | 现代版本能力层（Data Components、新 API） |
| `module-java17` | 17 | Java 17 专属依赖与实现 |
| `module-java21` | 21 | Java 21 专属依赖与实现 |

依赖规则：
- `module-legacy-api` 仅依赖 `common`，不依赖 `common-impl`
- `module-java17` / `module-java21` 仅依赖 `common`，独立 JVM Target
- 高版本依赖不允许泄漏到低版本模块

## 编译依赖

根 `build.gradle.kts` 中的 Bukkit 版本依赖：

```
v12004    # 1.20.4
v12110    # 1.21.10（1.21.1 的 NMS 映射）
v260100   # 26.1.0（未来版本）
```

## 版本适配编写原则

1. 公共 API（`common` 模块）保持最低兼容性，不引入高版本依赖
2. 版本差异通过 `VersionedService` 接口隔离
3. 运行时通过 `VersionAdapterResolver.resolveOrNull()` 动态装配正确实现
4. NMS 差异通过 TabooLib 的 `nmsProxy` 隔离
5. 禁止在业务代码中直接判断版本号
