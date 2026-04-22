// module-command: 命令与调试入口

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

taboolib { subproject = true }
