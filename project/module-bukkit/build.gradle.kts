// module-bukkit: Bukkit 平台入口，仅负责启动与桥接

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

taboolib { subproject = true }
