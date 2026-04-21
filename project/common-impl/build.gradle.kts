// common-impl: 默认实现、注册中心、加载器、Fluxon 运行时

val fluxonVersion: String by rootProject.extra
val fluxonPluginVersion: String by rootProject.extra

dependencies {
    compileOnly(project(":project:common"))
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("org.tabooproject.fluxon:core:$fluxonVersion")
    compileOnly("org.tabooproject.fluxon.plugin:core:$fluxonPluginVersion")
    compileOnly("org.tabooproject.fluxon.plugin:common:$fluxonPluginVersion")
    compileOnly("org.tabooproject.fluxon.plugin:platform-bukkit:$fluxonPluginVersion")
    compileOnly(fileTree("libs"))
}

taboolib { subproject = true }
