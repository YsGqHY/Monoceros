// common: 对外 API、事件、数据结构、抽象接口
// 最底层模块，不依赖任何其他 project 子模块

dependencies {
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
}

taboolib { subproject = true }
