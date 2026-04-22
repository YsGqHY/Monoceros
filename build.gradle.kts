import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.36" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.0" apply false
}

val fluxonVersion by extra("1.6.24")
val fluxonPluginVersion by extra("1.1.8")

subprojects {
    // :project 是纯聚合目录，不参与插件构建
    if (name == "project") return@subprojects

    apply(plugin = "java")
    apply(plugin = "io.izzel.taboolib")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    // TabooLib 全局配置，所有子模块共享
    configure<TabooLibExtension> {
        description {
            name(rootProject.name)
        }
        env {
            install(Basic, Bukkit, BukkitUtil, BukkitHook)
            install(Database)
            install(CommandHelper)
            install(I18n, MinecraftChat, MinecraftEffect)
            // Aether 解析器在部分仓库（如 Fluxon 仓库）上存在兼容性问题，
            // 强制使用 Legacy 下载器以保证 Fluxon 运行时能正常下载
            enableLegacyDependencyResolver = true
        }
        version {
            taboolib = "6.3.0-716e043"
        }
    }

    repositories {
        mavenCentral()
        maven("https://nexus.maplex.top/repository/maven-public/")
    }

    dependencies {
        compileOnly(kotlin("stdlib"))
        // Bukkit 核心依赖（全局共享，子模块无需重复声明）
        compileOnly("ink.ptms.core:v12004:12004:mapped")
        compileOnly("ink.ptms.core:v12004:12004:universal")
        compileOnly("ink.ptms.core:v12110:12110:mapped")
        compileOnly("ink.ptms.core:v12110:12110:universal")
        compileOnly("ink.ptms.core:v260100:260100")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
            freeCompilerArgs.addAll("-Xjvm-default=all")
        }
    }
}

// 根模块不产出构建产物，清理由各子模块自行管理
