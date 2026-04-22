// module-dispatcher: Bukkit 事件分发层

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

taboolib { subproject = true }
