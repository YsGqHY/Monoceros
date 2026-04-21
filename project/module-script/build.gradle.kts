// module-script: 脚本定义、预热、缓存、动态加载

val fluxonVersion: String by rootProject.extra
val fluxonPluginVersion: String by rootProject.extra

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly("org.tabooproject.fluxon:core:$fluxonVersion")
    compileOnly("org.tabooproject.fluxon.plugin:core:$fluxonPluginVersion")
    compileOnly("org.tabooproject.fluxon.plugin:common:$fluxonPluginVersion")
    compileOnly("org.tabooproject.fluxon.plugin:platform-bukkit:$fluxonPluginVersion")
}

taboolib { subproject = true }
