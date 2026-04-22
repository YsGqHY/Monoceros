// module-modern: 面向现代版本的数据组件与新版能力

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

taboolib { subproject = true }
