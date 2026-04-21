package cc.bkhk.monoceros.api.command

/**
 * 命令服务
 */
interface CommandService {
    fun register(definition: CommandDefinition)
    fun unregister(id: String)
    fun reloadAll(): Int
}
