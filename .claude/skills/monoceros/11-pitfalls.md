# Monoceros 常见陷阱与编写规范

## 配置编写陷阱

### 1. 忘记 resource-version

所有 YAML 配置文件必须包含 `resource-version: 1`：

```yaml
# 正确
resource-version: 1
id: my.dispatcher
listen-event: PlayerJoinEvent
# ...

# 错误 -- 缺少 resource-version
id: my.dispatcher
listen-event: PlayerJoinEvent
```

### 2. 路由字段名不一致

事件分发器、调度、命令使用 `execute` 包裹路由，数据包监听使用 `route`：

```yaml
# 分发器/调度/命令 -- 使用 execute
execute:
  route: script
  value: my.script

# 数据包监听 -- 使用 route（不包裹在 execute 中）
route:
  type: script
  value: my.script
```

### 3. 时间单位混淆

`delay` 和 `period` 在调度配置中解析为 tick，`max-duration-ms` 解析为毫秒：

```yaml
# 正确
delay: 20t          # 20 tick = 1 秒
period: 5s          # 5 秒 = 100 tick
max-duration-ms: 1h # 1 小时 = 3600000 毫秒

# 错误 -- 无单位时默认行为不同
delay: 20           # 20 tick（parseTicks 上下文）
max-duration-ms: 20 # 20 毫秒（parseMs 上下文），可能不是预期
```

### 4. wireshark 拦截/覆写未开启主开关

即使配置了 `intercept: true` 或 `rewrite`，如果主配置未开启对应开关，也不会生效：

```yaml
# config.yml 中必须开启
wireshark:
  allow-intercept: true    # 才能让 tap 的 intercept 生效
  allow-rewrite: true      # 才能让 tap 的 rewrite 生效
```

### 5. 事件名使用全限定名

`listen-event` 应使用事件类简名，不是全限定类名：

```yaml
# 正确
listen-event: PlayerJoinEvent

# 错误
listen-event: org.bukkit.event.player.PlayerJoinEvent
```

### 6. 脚本 ID 与文件路径不匹配

脚本 ID 由路径推导，确保路由中引用的 ID 与实际文件路径一致：

```yaml
# 文件: script/dispatcher/player-join.fs
# 推导 ID: dispatcher.player-join

# 正确
execute:
  route: script
  value: dispatcher.player-join

# 错误
execute:
  route: script
  value: player-join              # 缺少目录前缀
  value: dispatcher/player-join   # 使用了路径分隔符而非点号
```

### 7. 工作流节点 type 未注册

节点的 `type` 必须是已注册的 ActionNode 类型。15 个内建类型：
`script`、`set`、`log`、`wait`、`branch`、`loop`、`sound`、`tellraw`、`regex`、`try-catch`、`input`、`if-else`、`math`、`coerce`、`dispatch`

```yaml
# 错误 -- 不存在的节点类型
- id: my-node
  type: execute-command    # 未注册的类型
```

### 8. 调度类型与字段不匹配

不同调度类型需要不同的字段：

```yaml
# DELAY 类型 -- 只需 delay
type: delay
delay: 5s

# PERIODIC 类型 -- 需要 delay + period
type: periodic
delay: 20t
period: 200t

# CRON 类型 -- 需要 cron
type: cron
cron: "0 0 0 * * ?"

# 错误 -- DELAY 类型配了 period（无效）
type: delay
delay: 5s
period: 200t    # 被忽略
```

## Fluxon 脚本陷阱

### 9. 裸标识符不是变量引用

Fluxon 中裸标识符是字符串字面量，读取变量必须用 `&` 或 `&?`：

```fluxon
// 正确
name = &?player
print("玩家: ${&?player}")

// 错误 -- player 是字符串 "player"，不是变量
name = player
print("玩家: ${player}")
```

### 10. 使用 & 而非 &? 访问可能不存在的变量

系统变量可能为 null（如控制台执行时 `player` 为 null），应使用安全引用：

```fluxon
// 正确 -- 安全引用，不存在时返回 null
name = &?player ?: "控制台"

// 危险 -- 变量不存在时报错
name = &player
```

### 11. 忘记 Fluxon 的字符串插值语法

Fluxon 字符串插值使用 `${}`，内部引用变量仍需 `&`：

```fluxon
// 正确
print("伤害: ${&?damage}")

// 错误 -- damage 是字符串字面量
print("伤害: ${damage}")
```

## 配置组织规范

### 20. 文件命名

- 使用小写字母和连字符：`player-join.yml`、`combat-hit.yml`
- 避免空格和特殊字符
- 以 `#` 开头的文件会被忽略（可用于临时禁用）

### 21. ID 命名

- 使用点分隔的层级命名：`player.join.welcome`、`combat.hit-feedback`
- 保持与文件目录结构的逻辑对应关系
- 避免过长的 ID

### 22. 变量命名

- `variables` 中的键使用 camelCase：`triggerSource`、`customKey`
- 避免与系统保留变量冲突：`sender`、`player`、`source`、`scriptId`、`now`、`thread`

### 23. 注释规范

- YAML 配置中使用 `#` 注释说明用途
- Fluxon 脚本中使用 `//` 注释
- 在文件头部注明脚本 ID 和被引用关系

```fluxon
// 脚本 ID: dispatcher.player-join
// 被 dispatcher/player-join.yml 引用

print("玩家 ${&?player} 加入了服务器")
```

## Fluxon 语言陷阱

### 12. `::` 左侧语法糖

`time :: now()` 等价于 `time() :: now()`，裸标识符被当作函数调用。变量必须加 `&`：

```fluxon
// 正确
&list :: size()

// 错误 -- list 被当作函数调用 list()
list :: size()
```

### 13. `..` 与 `..<` 区间

`1..5` 是闭区间（含 5），`0..<10` 是左闭右开（不含 10）：

```fluxon
for i in 1..5 { print(&i) }     // 输出 1,2,3,4,5
for i in 0..<5 { print(&i) }    // 输出 0,1,2,3,4
```

### 14. 函数不能嵌套

`def` 只能在顶层定义，内部逻辑用 Lambda：

```fluxon
// 正确
helper = |x| &x * 2
def process(n) = call(&helper, [&n])

// 错误 -- def 不能嵌套
def outer() {
    def inner() = ...    // ❌
}
```

### 15. `.` 与 `::` 的区别

`.` 是 Java 反射访问（需 `allowReflectionAccess` 权限），`::` 是 Fluxon 扩展函数。优先用 `::`：

```fluxon
// 推荐 -- 扩展函数
&text :: length()
&list :: size()

// 仅在需要 Java 原生方法时使用反射
&text.toUpperCase()
```

### 16. 全大写标识符自动成为常量

`MAX = 10` 后 `MAX = 20` 会报错，因为全大写标识符赋值字面量时自动成为编译时常量：

```fluxon
MAX = 10
MAX = 20    // ❌ Cannot reassign constant: MAX
```

### 17. `static`/`new` 需要宿主权限

宿主未开启 `allowJavaConstruction` 时，`static` 和 `new` 关键字会失败。

### 17.5. Kotlin `object` 需要 `@JvmStatic` 才能被 Fluxon `static` 直接调用

Kotlin `object` 的方法默认不是 Java 静态方法。如果没有标注 `@JvmStatic`，Fluxon 的 `static` 关键字找不到方法，必须通过 `INSTANCE` 访问：

```fluxon
// 没有 @JvmStatic 时 -- 必须通过 INSTANCE
obj = static (com.example.MyObject).INSTANCE.doSomething()

// 有 @JvmStatic 时 -- 可直接调用
obj = static com.example.MyObject.doSomething()
```

`Monoceros` 的公开方法已标注 `@JvmStatic`，可直接调用：

```fluxon
// 正确
api = static cc.bkhk.monoceros.Monoceros.api()
plugin = static cc.bkhk.monoceros.Monoceros.plugin()
```

注意 `Monoceros` 类在 `cc.bkhk.monoceros` 包下，不在 `cc.bkhk.monoceros.api` 包下：

```fluxon
// 错误 -- 包路径错误，ClassNotFoundException
api = static cc.bkhk.monoceros.api.Monoceros.api()
```

### 18. `runAsync`/`runSync` 仅在 `scope {}` 内有效

```fluxon
// 正确
future = scope {
    runAsync { heavyComputation() }
}

// 错误 -- 不在 scope 内
runAsync { heavyComputation() }    // ❌
```

### 19. Fluxon 没有 `!!` 操作符

空安全使用 `&?name`、`?::`、`?.`、`?:` 组合：

```fluxon
name = &?username ?: "anonymous"
&?player ?:: getName()
&?obj?.method()
```

## Fluxon 编写规范

1. 变量读取始终用 `&`，可能为 null 用 `&?`
2. 优先 `::` 扩展函数，非必要不用 `.` 反射
3. `when` 替代多层 `if-else`
4. 单参 Lambda 用 `|| &it` 隐式参数
5. 全大写命名常量（编译时内联）
6. 字符串插值 `"${expr}"` 替代手动拼接
7. 函数内部逻辑用 Lambda，不要尝试嵌套 `def`

## 安全规范

### 24. wireshark 高风险操作

- `intercept` 和 `rewrite` 默认关闭，开启前确认影响范围
- 生产环境慎用数据包拦截，可能导致客户端异常
- 建议先用 `tracking: true` 观察，确认无误后再启用拦截/覆写

### 25. 异步安全

- 异步脚本中禁止直接修改玩家背包、世界状态、实体状态
- 需要修改 Bukkit 状态时，使用 `submit {}` 回到主线程
- 调度配置中 `async: true` 时注意线程安全

### 26. 热重载注意事项

- 修改配置后等待文件监听节流间隔（默认 500ms）再验证
- 脚本编译失败不会覆盖已缓存的可用版本
- 重载顺序：脚本(100) > 分发器(50) = 调度(50) > 数据包(40) > 工作流(30)
- 如果脚本被分发器引用，确保脚本先于分发器加载完成
