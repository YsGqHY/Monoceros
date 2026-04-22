// module-legacy-api: 面向旧版本 / 旧物料体系 / Java 8 的兼容层

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

taboolib { subproject = true }
