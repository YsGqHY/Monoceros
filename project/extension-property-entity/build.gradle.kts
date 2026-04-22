// extension-property-entity: 实体属性域扩展

dependencies {
    compileOnly(project(":project:common"))
    compileOnly(project(":project:common-impl"))
    compileOnly(project(":project:workflow-property"))
}

taboolib { subproject = true }
