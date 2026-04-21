// module-dispatcher: Bukkit 事件分发层

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
}

taboolib { subproject = true }
