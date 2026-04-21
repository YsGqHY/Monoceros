package cc.bkhk.monoceros.api.script

import cc.bkhk.monoceros.api.registry.Registry

/**
 * 脚本定义注册表
 *
 * 管理所有已加载的脚本定义，由资源加载层维护。
 */
interface ScriptDefinitionRegistry : Registry<ScriptDefinition>
