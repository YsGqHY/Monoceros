import io.izzel.taboolib.gradle.*

// plugin: 最终聚合打包模块

taboolib {
    description {
        name(rootProject.name)
    }
    env {
        install(BukkitNMS, BukkitNMSUtil)
    }
    relocate("org.tabooproject.fluxon", "cc.bkhk.monoceros.engine.fluxon")
}

tasks {
    jar {
        // 聚合所有子模块产物，排除纯聚合目录 :project
        archiveBaseName.set(rootProject.name)
        rootProject.subprojects.filter { it.name != "project" }.forEach {
            from(it.sourceSets["main"].output)
        }
    }
}
