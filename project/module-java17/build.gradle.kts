// module-java17: 当依赖必须要求 Java 17 单独编译时使用

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

taboolib { subproject = true }
