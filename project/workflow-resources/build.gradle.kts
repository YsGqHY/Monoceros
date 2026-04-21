// workflow-resources: 默认配置、模板、语言文件、示例资源

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

taboolib { subproject = true }
