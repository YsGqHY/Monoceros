// module-volatility: NMS / 发包 / 伪装 / 挥发性能力

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

taboolib {
    subproject = true
    env {
        install(io.izzel.taboolib.gradle.BukkitNMS, io.izzel.taboolib.gradle.BukkitNMSUtil)
    }
}
