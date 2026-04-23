# Monoceros 主配置参考

## 文件位置

`plugins/Monoceros/config.yml`

## 完整字段定义

```yaml
# Monoceros 主配置文件
# 修改后可通过 /monoceros reload 热重载
resource-version: 1

# 调试模式
# true: 输出详细诊断日志（DiagnosticLogger 的 debug 级别消息会输出）
# false: 仅输出关键信息
debug: false

# 脚本系统配置
script:
  # 文件监听节流间隔（毫秒）
  # 脚本文件变更后，等待此间隔再触发重载，避免频繁 IO
  # 设为 0 或负数则不启用文件监听（需手动 reload）
  watcher-throttle-ms: 500

# 数据包系统配置
wireshark:
  # 是否允许 packet 拦截
  # true: 允许 wireshark 配置中的 tap 取消原始数据包
  # false: 即使 tap 配置了 intercept: true，也不会生效
  # 高风险操作，默认关闭
  allow-intercept: false

  # 是否允许 packet 覆写
  # true: 允许 wireshark 配置中的 tap 替换原始数据包
  # false: 即使 tap 配置了 rewrite，也不会生效
  # 高风险操作，默认关闭
  allow-rewrite: false
```

## 字段速查表

| 字段路径 | 类型 | 默认值 | 说明 |
|----------|------|--------|------|
| `resource-version` | int | `1` | 配置格式版本号 |
| `debug` | boolean | `false` | 调试模式开关 |
| `script.watcher-throttle-ms` | int | `500` | 脚本文件监听节流间隔（毫秒），<=0 不启用 |
| `wireshark.allow-intercept` | boolean | `false` | 是否允许数据包拦截 |
| `wireshark.allow-rewrite` | boolean | `false` | 是否允许数据包覆写 |

## 配置读取机制

主配置通过 TabooLib 的 `@Config` 注解自动加载，支持 `@ConfigNode` 绑定到字段。配置变更后通过 LiveData 响应式系统自动传播到依赖方。

LiveData 支持的转换链：

| 转换方法 | 说明 |
|----------|------|
| `.boolean()` | 转为 Boolean |
| `.int()` | 转为 Int |
| `.long()` | 转为 Long |
| `.float()` | 转为 Float |
| `.double()` | 转为 Double |
| `.string()` | 转为 String |
| `.list()` | 转为 List |
| `.map()` | 转为 Map |
| `.stringList()` | 转为 List<String> |
| `.intList()` | 转为 List<Int> |
| `.mapList()` | 转为 List<Map> |
| `.normalizeMap()` | 规范化 Map 键 |
| `.applicative<R>()` | 通过 Applicative 注册中心转换 |
| `.default(value)` | 设置默认值 |
| `.exceptionally(handler)` | 异常兜底 |
