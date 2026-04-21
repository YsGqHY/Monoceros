// module-legacy-api: 面向旧版本 / 旧物料体系 / Java 8 的兼容层

dependencies {
    compileOnly(project(":project:common"))
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
}

taboolib { subproject = true }
