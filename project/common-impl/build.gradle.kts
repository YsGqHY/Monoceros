// common-impl: 默认实现、注册中心、加载器、Fluxon 运行时

val fluxonVersion: String by rootProject.extra
val fluxonPluginVersion: String by rootProject.extra

dependencies {
    compileOnly(project(":project:common"))
    compileOnly("org.tabooproject.fluxon:core:$fluxonVersion")
    compileOnly("org.tabooproject.fluxon.plugin:core:$fluxonPluginVersion")
    compileOnly("org.tabooproject.fluxon.plugin:common:$fluxonPluginVersion")
    compileOnly("org.tabooproject.fluxon.plugin:platform-bukkit:$fluxonPluginVersion")
    // Gson 由 Minecraft 服务端运行时提供，仅编译期需要
    compileOnly("com.google.code.gson:gson:2.11.0")
    compileOnly("org.ow2.asm:asm:9.8")
    compileOnly("org.ow2.asm:asm-util:9.8")
    compileOnly("org.ow2.asm:asm-commons:9.8")
    compileOnly(fileTree("libs"))
}

taboolib { subproject = true }
