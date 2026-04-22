// extension-action-event: 事件域动作扩展

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
    compileOnly(project(":project:workflow-action"))
}

taboolib { subproject = true }
