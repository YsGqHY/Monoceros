// module-script: 脚本定义、预热、缓存、动态加载

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

taboolib { subproject = true }
