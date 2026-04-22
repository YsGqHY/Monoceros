// extension-action-illusion: 幻象域动作扩展

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
    compileOnly(project(":project:workflow-action"))
    compileOnly(project(":project:module-volatility"))
}

taboolib { subproject = true }
