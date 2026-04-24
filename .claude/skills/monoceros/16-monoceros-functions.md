# Monoceros 扩展函数参考

Monoceros 在 Fluxon 运行时基础上注册了一组扩展函数，覆盖 JSON、HTTP、UUID、冷却、颜色文本、日志、正则表达式。所有函数在 ENABLE 阶段自动注册，脚本中直接调用，无需 import。

---

## 1. JSON — jsonParse / jsonStringify

底层基于 Gson（Minecraft 服务端自带）。

### 顶层函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `jsonParse(str)` | `String -> Any?` | 解析 JSON 字符串，自动解包基本类型；对象返回 JsonObject，数组返回 JsonArray |
| `jsonStringify(obj)` | `Any? -> String` | 序列化为紧凑 JSON |
| `jsonPretty(obj)` | `Any? -> String` | 序列化为格式化 JSON |
| `jsonObject()` | `-> JsonObject` | 创建空 JsonObject |
| `jsonArray()` | `-> JsonArray` | 创建空 JsonArray |
| `json()` | `-> JsonApi` | 获取 JSON 工具对象（可通过 `::` 调用其 @Export 方法） |

### JsonObject 扩展函数

| 函数 | 返回类型 | 说明 |
|------|----------|------|
| `get(key)` | `Any?` | 获取字段值，自动解包 |
| `getString(key)` | `String?` | 获取字符串 |
| `getInt(key)` | `int` | 获取整数，缺失返回 0 |
| `getLong(key)` | `long` | 获取长整数，缺失返回 0 |
| `getDouble(key)` | `double` | 获取浮点数，缺失返回 0.0 |
| `getBoolean(key)` | `boolean` | 获取布尔值，缺失返回 false |
| `getObject(key)` | `JsonObject?` | 获取嵌套对象 |
| `getArray(key)` | `JsonArray?` | 获取嵌套数组 |
| `has(key)` | `boolean` | 是否包含字段 |
| `set(key, value)` | `void` | 设置字段，自动包装基本类型 |
| `remove(key)` | `void` | 移除字段 |
| `keys()` | `List<String>` | 所有字段名 |
| `size()` | `int` | 字段数量 |
| `toMap()` | `Map<String, Any?>` | 转为 Fluxon 原生 Map |

### JsonArray 扩展函数

| 函数 | 返回类型 | 说明 |
|------|----------|------|
| `get(index)` | `Any?` | 按索引获取，自动解包 |
| `size()` | `int` | 元素数量 |
| `add(value)` | `void` | 添加元素 |
| `toList()` | `List<Any?>` | 转为 Fluxon 原生 List |

### 示例

```fluxon
// 解析
obj = jsonParse('{"name":"Monoceros","version":1,"tags":["mc","plugin"]}')
name = &obj :: getString("name")       // "Monoceros"
ver = &obj :: getInt("version")         // 1
tags = &obj :: getArray("tags")
&tags :: toList() :: each(|| print(&it))

// 构建
obj = jsonObject()
&obj :: set("player", "Steve")
&obj :: set("score", 100)
text = jsonStringify(&obj)              // {"player":"Steve","score":100}

// 嵌套
data = jsonParse('{"user":{"name":"Alex","level":5}}')
user = &data :: getObject("user")
print(&user :: getString("name"))       // "Alex"
```

---

## 2. HTTP — httpGet / httpPost / httpRequest

底层基于 JDK HttpURLConnection，所有请求强制异步（返回 `CompletableFuture`），脚本侧通过 `await` 消费。默认 5s 连接超时 + 10s 读取超时。

### 顶层函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `httpGet(url)` | `String -> CompletableFuture<HttpResponse>` | GET 请求 |
| `httpPost(url, body)` | `(String, String) -> CompletableFuture<HttpResponse>` | POST 请求（Content-Type: application/json） |
| `httpRequest(url, method, headers, body)` | `(String, String, Map, String?) -> CompletableFuture<HttpResponse>` | 通用请求 |

### HttpResponse 扩展函数

| 函数 | 返回类型 | 说明 |
|------|----------|------|
| `statusCode()` | `int` | HTTP 状态码 |
| `body()` | `String` | 响应体文本 |
| `header(name)` | `String?` | 获取响应头（name 不区分大小写） |
| `headers()` | `Map<String, String>` | 所有响应头 |
| `isOk()` | `boolean` | 状态码是否在 200~299 |

### 示例

```fluxon
// GET + JSON 解析
resp = await httpGet("https://v1.hitokoto.cn")
if &resp :: isOk() {
    data = jsonParse(&resp :: body())
    print(&data :: getString("hitokoto"))
}

// POST
body = jsonStringify(["name": "test", "value": 42])
resp = await httpPost("https://example.com/api", &body)
print(&resp :: statusCode())

// 自定义请求
headers = ["Authorization": "Bearer token123", "Accept": "application/json"]
resp = await httpRequest("https://example.com/api", "PUT", &headers, '{"data":1}')
print(&resp :: body())
```

### 注意事项

- 所有 HTTP 函数返回 `CompletableFuture`，必须用 `await` 等待结果
- 在 `async def` 函数中可直接 `await`
- 在普通脚本顶层也可直接 `await`（Fluxon 会自动阻塞当前脚本线程等待）
- 建议在调度配置中设置 `async: true`，避免阻塞主线程

---

## 3. UUID — uuid / uuidFromString / uuidFromName

封装 `java.util.UUID`。

### 顶层函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `uuid()` | `-> UUID` | 随机生成 UUID（v4） |
| `uuidFromString(str)` | `String -> UUID?` | 从标准格式解析，格式错误返回 null |
| `uuidFromName(name)` | `String -> UUID` | 基于名称生成（v3，MD5） |

### UUID 扩展函数

| 函数 | 返回类型 | 说明 |
|------|----------|------|
| `version()` | `int` | UUID 版本号 |
| `mostBits()` | `long` | 高 64 位 |
| `leastBits()` | `long` | 低 64 位 |

### 示例

```fluxon
// 随机生成
id = uuid()
print(&id)                              // "550e8400-e29b-41d4-a716-446655440000"

// 从字符串解析
id = uuidFromString("550e8400-e29b-41d4-a716-446655440000")
if &id != null {
    print(&id :: version())             // 4
}

// 基于名称生成（同一名称始终生成相同 UUID）
id = uuidFromName("Steve")
print(&id)
```

---

## 4. 冷却 — cooldown / hasCooldown / getCooldown

基于 `ConcurrentHashMap<String, Long>` 实现，线程安全。ACTIVE 阶段自动启动过期条目清理任务（约 5 分钟一次）。

### 顶层函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `cooldown(key, durationMs)` | `(String, long) -> boolean` | 设置冷却；返回 true 表示成功设置（之前不在冷却中），false 表示仍在冷却 |
| `hasCooldown(key)` | `String -> boolean` | 是否在冷却中 |
| `getCooldown(key)` | `String -> long` | 剩余冷却毫秒数，不在冷却中返回 0 |
| `removeCooldown(key)` | `String -> void` | 移除冷却 |
| `clearCooldowns()` | `-> void` | 清空所有冷却 |

### 示例

```fluxon
// 技能冷却（key 由脚本侧自行拼接）
key = &?player.getName() + ":skill:fireball"

if cooldown(&key, 3000) {
    // 成功设置冷却，执行技能
    print("火球术释放!")
} else {
    // 仍在冷却中
    remain = getCooldown(&key)
    print("冷却中，剩余 ${&remain} ms")
}

// 交互防抖
if cooldown("interact:" + &?player.getName(), 500) {
    // 处理交互
}

// 强制重置
removeCooldown(&key)
```

---

## 5. 颜色文本 — colored / uncolored

封装 TabooLib MinecraftChat 模块的颜色代码处理。

### 顶层函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `colored(text)` | `String -> String` | 颜色代码转换（`&a` → `§a`，支持 HEX） |
| `uncolored(text)` | `String -> String` | 去除所有颜色代码 |

### String 扩展函数

| 函数 | 说明 |
|------|------|
| `:: colored()` | 等价于 `colored(text)` |
| `:: uncolored()` | 等价于 `uncolored(text)` |

### 示例

```fluxon
// 顶层函数
msg = colored("&a[成功] &f操作完成")
&?player?.sendMessage(&msg)

// 扩展函数
"&e[提示] &f你好" :: colored()

// 去除颜色
plain = uncolored(&msg)

// 组合使用
prefix = "&6[Monoceros]" :: colored()
&?player?.sendMessage(&prefix + " 欢迎!")
```

---

## 6. 日志 — logInfo / logWarn / logDebug / logError

封装 Monoceros DiagnosticLogger，输出带 `[Script]` 模块前缀的结构化日志。

### 顶层函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `logInfo(msg)` | `String -> void` | 输出信息日志 |
| `logWarn(msg)` | `String -> void` | 输出警告日志 |
| `logDebug(msg)` | `String -> void` | 仅调试模式输出（需 `config.yml` 开启 debug） |
| `logError(msg)` | `String -> void` | 输出错误日志 |

### 示例

```fluxon
logInfo("玩家 ${&?player.getName()} 触发了事件")
logWarn("配置项缺失，使用默认值")
logDebug("变量快照: key=${&key}, value=${&value}")
logError("外部 API 调用失败")
```

---

## 7. 正则表达式 — regex / regexMatch / regexMatchAll / regexReplace

封装 `java.util.regex`，提供独立 Regex 对象（捕获组、编译复用、分割等），补充 Fluxon String 内置 `matches`/`findAll` 的不足。

### 顶层函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `regex(pattern)` | `String -> RegexWrapper` | 编译正则表达式，返回可复用对象 |
| `regexMatch(text, pattern)` | `(String, String) -> MatchWrapper?` | 快捷匹配首个 |
| `regexMatchAll(text, pattern)` | `(String, String) -> List<MatchWrapper>` | 快捷全局匹配 |
| `regexReplace(text, pattern, replacement)` | `(String, String, String) -> String` | 快捷替换全部 |

### RegexWrapper 扩展函数

| 函数 | 签名 | 说明 |
|------|------|------|
| `test(text)` | `String -> boolean` | 是否匹配 |
| `match(text)` | `String -> MatchWrapper?` | 首个匹配 |
| `matchAll(text)` | `String -> List<MatchWrapper>` | 全局匹配 |
| `replace(text, replacement)` | `(String, String) -> String` | 替换首个 |
| `replaceAll(text, replacement)` | `(String, String) -> String` | 替换全部 |
| `split(text)` | `String -> List<String>` | 按正则分割 |
| `pattern()` | `-> String` | 获取正则表达式字符串 |

### MatchWrapper 扩展函数

| 函数 | 返回类型 | 说明 |
|------|----------|------|
| `value()` | `String` | 匹配到的完整文本 |
| `group(index)` | `String?` | 按索引获取捕获组（0 为完整匹配） |
| `groupCount()` | `int` | 捕获组数量 |
| `start()` | `int` | 匹配起始位置 |
| `end()` | `int` | 匹配结束位置 |
| `groups()` | `List<String?>` | 所有捕获组列表 |

### 示例

```fluxon
// 快捷函数
m = regexMatch("Hello 123 World 456", "\\d+")
print(&m :: value())                    // "123"

all = regexMatchAll("Hello 123 World 456", "\\d+")
&all :: each(|| print(&it :: value()))  // "123", "456"

result = regexReplace("Hello World", "(\\w+)", "[$1]")
print(&result)                          // "[Hello] [World]"

// 编译正则对象（可复用，避免重复编译）
re = regex("(\\d{4})-(\\d{2})-(\\d{2})")
m = &re :: match("日期: 2024-01-15")
if &m != null {
    print(&m :: group(1))               // "2024"
    print(&m :: group(2))               // "01"
    print(&m :: group(3))               // "15"
    print(&m :: groups())               // ["2024-01-15", "2024", "01", "15"]
}

// 测试
if &re :: test("2024-12-25") {
    print("日期格式正确")
}

// 分割
parts = regex("\\s*,\\s*") :: split("a, b , c,d")
// ["a", "b", "c", "d"]
```

---

## 综合示例：每日一言

```fluxon
// 从 hitokoto.cn 获取每日一言并广播

resp = await httpGet("https://v1.hitokoto.cn")
if &resp :: isOk() {
    data = jsonParse(&resp :: body())
    hitokoto = &data :: getString("hitokoto")
    from = &data :: getString("from")
    author = &data :: getString("from_author")

    msg = "&e[每日一言] &f" + &hitokoto
    if &from != null {
        msg = &msg + " &7—— " + (&?author ?: "") + "《" + &from + "》"
    }
    &?player?.sendMessage(&msg :: colored())
    logInfo("每日一言已发送: ${&hitokoto}")
} else {
    logWarn("每日一言 API 请求失败: ${&resp :: statusCode()}")
}
```
