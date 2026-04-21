// module-wireshark: Packet 收发、过滤、匹配、追踪、拦截、覆写

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
}

taboolib {
    subproject = true
    env {
        install(io.izzel.taboolib.gradle.BukkitNMS, io.izzel.taboolib.gradle.BukkitNMSUtil)
    }
}
