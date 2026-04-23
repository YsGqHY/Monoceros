# Fluxon JVM 互操作参考

## 1. 成员访问 `.`（Java 反射）

基于 Java 反射，console/REPL 默认启用，嵌入式需显式开启 `allowReflectionAccess`。

```fluxon
text = "hello"
&text.length()                             // 5
&text.toUpperCase()                        // "HELLO"
```

### 安全成员访问 `?.`

target 为 null 时返回 null，不访问成员：

```fluxon
&maybeNull?.length()                       // null 时返回 null
&maybeNull?.method()                       // null 时不调用
```

### Class 对象特殊处理

当 target 是 `Class` 对象时，优先查找该类的静态方法，找不到时回退到 `java.lang.Class` 的实例方法：

```fluxon
intClass = forName("java.lang.Integer")
&intClass.parseInt("123")                  // 静态方法 → 123
&intClass.getName()                        // 回退到 Class 实例方法 → "java.lang.Integer"
```

---

## 2. 静态成员访问 `static`

需要宿主启用 `allowJavaConstruction`。

```fluxon
// 静态方法调用
static java.lang.Integer.parseInt("42")     // 42

// 静态字段访问
static java.lang.Math.PI                    // 3.14159...
static java.lang.Integer.MAX_VALUE          // 2147483647

// 链式调用
static java.lang.System.out.println("hello")

// 括号语法消除歧义（类名后需要继续链式调用时）
static (java.lang.Integer).TYPE.getName()   // "int"
static (com.example.MyObject).INSTANCE.doSomething()
```

---

## 3. `new` 关键字（Java 对象构造）

需要宿主启用 `allowJavaConstruction`。

```fluxon
list = new java.util.ArrayList()
list = new java.util.ArrayList(10)
sb = new java.lang.StringBuilder("hello") :: toString()
```

支持后缀链式调用。

---

## 4. `impl` 关键字（匿名接口/类实现）

动态实现 Java 接口或继承抽象类。

### 实现接口

```fluxon
runnable = impl: java.lang.Runnable {
    override run {
        print("running")
    }
}
```

### 继承类 + 实现多接口

```fluxon
myObj = impl: java.util.AbstractList(superArgs), java.io.Serializable {
    override get(index) = ...
    override size = ...
}
```

### 方法体形式

```fluxon
impl: SomeInterface {
    override method1 = expression          // 表达式体
    override method2(a, b) {               // 块体
        return &a + &b
    }
}
```

---

## 5. 并发与异步

### async / sync 函数

```fluxon
// 异步函数返回 CompletableFuture
async def fetchData() = {
    sleep(1000)
    "data loaded"
}

// 主线程函数（需宿主运行时支持）
sync def updateUI() = {
    print("UI updated")
}
```

### await

等待 CompletableFuture 完成并获取结果：

```fluxon
result = await fetchData()
result = await &futureVariable
```

### 结构化并发

```fluxon
// scope 返回 CompletableFuture
future = scope {
    runAsync {
        // 在线程池中执行
        heavyComputation()
    }
    runSync {
        // 在主线程执行
        updateState()
    }
}
result = await &future
```

`runAsync` / `runSync` 仅在 `scope {}` 内有效。

### Domain 表达式

```fluxon
// with：执行闭包，返回最后一行
result = &obj :: with {
    doSomething()
    computeResult()
}

// also：执行闭包，返回 target 本身
list = [1, 2, 3] :: also {
    print("created list")
}
// list 仍然是 [1, 2, 3]
```

---

## 6. 注解系统

### 脚本注解

注解只能应用于函数定义（`def` 之前）：

```fluxon
// 无属性
@api
def add(a, b) = &a + &b

// 带属性（值支持字面量、字符串、数字、列表、映射）
@annotation(name="demo", value=1)
def fib(n) = if &n <= 1 then 1 else fib(&n - 1) + fib(&n - 2)
```

### @api 注解

标记函数为库导出函数：

```fluxon
// 注册为全局函数
@api
def greet(name) = "Hello, " + &name

// 注册为指定类型的扩展函数
@api(bind="java.lang.String")
def shout() = this() :: uppercase() + "!"
// 使用：&text :: shout()
```

### 自定义注解

注解是开放式的，用户可自定义任意注解名：

```fluxon
@deprecated(reason="use newMethod instead")
def oldMethod() = ...

@permission(level="admin")
def dangerousOp() = ...
```

宿主 Java 代码通过 `AnnotationAccess.hasAnnotation(function, name)` 检查注解。

### Java 侧 @Export 注解

用于 Java 类方法，通过 `ExportRegistry.registerClass()` 自动注册为 Fluxon 扩展函数：

```java
public class TimeObject {
    @Export
    public long now() { return System.currentTimeMillis(); }

    @Export(async = true)   // 异步执行
    public CompletableFuture<String> fetchData() { ... }

    @Export(shared = true)  // 跨 ClassLoader 共享
    public void sharedFunc() { ... }

    public void helper(@Optional String charset) { ... }  // 可选参数
}
```

### 库加载系统

通过 `FluxonRuntime.loadLibrary(path)` 加载 `.fs` 文件作为库，`@api` 标记的函数自动注册到运行时。支持 `reload` 和 `unload`。
