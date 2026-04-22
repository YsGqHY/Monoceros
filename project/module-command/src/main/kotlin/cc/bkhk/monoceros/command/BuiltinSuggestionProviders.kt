package cc.bkhk.monoceros.command

import cc.bkhk.monoceros.api.command.CommandContext
import cc.bkhk.monoceros.api.command.SuggestionProvider
import cc.bkhk.monoceros.api.script.ScriptDefinitionRegistry
import org.bukkit.Bukkit
import org.bukkit.Material
import taboolib.common.platform.PlatformFactory

internal object OnlinePlayerSuggestionProvider : SuggestionProvider {
    override val id: String = "online-player"
    override fun suggest(context: CommandContext): List<String> = Bukkit.getOnlinePlayers().map { it.name }.sorted()
}

internal object OfflinePlayerSuggestionProvider : SuggestionProvider {
    override val id: String = "offline-player"
    override fun suggest(context: CommandContext): List<String> = Bukkit.getOfflinePlayers().mapNotNull { it.name }.sorted()
}

internal object WorldSuggestionProvider : SuggestionProvider {
    override val id: String = "world"
    override fun suggest(context: CommandContext): List<String> = Bukkit.getWorlds().map { it.name }.sorted()
}

internal object MaterialSuggestionProvider : SuggestionProvider {
    override val id: String = "material"
    override fun suggest(context: CommandContext): List<String> = Material.entries.map { it.name.lowercase() }.sorted()
}

internal object ScriptIdSuggestionProvider : SuggestionProvider {
    override val id: String = "script-id"
    override fun suggest(context: CommandContext): List<String> {
        val registry = PlatformFactory.getAPIOrNull<ScriptDefinitionRegistry>() ?: return emptyList()
        return registry.keys().sorted()
    }
}

internal object DispatcherIdSuggestionProvider : SuggestionProvider {
    override val id: String = "dispatcher-id"
    override fun suggest(context: CommandContext): List<String> {
        return reflectStringKeys(PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.dispatcher.DispatcherService>(), "dispatchers")
    }
}

internal object ScheduleIdSuggestionProvider : SuggestionProvider {
    override val id: String = "schedule-id"
    override fun suggest(context: CommandContext): List<String> {
        val service = PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.schedule.ScheduleService>() ?: return emptyList()
        val registry = reflectField(service, "definitionRegistry")
        return reflectRegistryKeys(registry)
    }
}

internal object WorkflowIdSuggestionProvider : SuggestionProvider {
    override val id: String = "workflow-id"
    override fun suggest(context: CommandContext): List<String> {
        val service = PlatformFactory.getAPIOrNull<cc.bkhk.monoceros.api.workflow.ActionWorkflowService>() ?: return emptyList()
        return reflectStringKeys(service, "definitions")
    }
}

private fun reflectStringKeys(instance: Any?, fieldName: String): List<String> {
    val field = reflectField(instance, fieldName)
    return when (field) {
        is Map<*, *> -> field.keys.map { it.toString() }.sorted()
        else -> emptyList()
    }
}

private fun reflectRegistryKeys(instance: Any?): List<String> {
    return runCatching {
        val method = instance?.javaClass?.methods?.firstOrNull { it.name == "keys" && it.parameterCount == 0 } ?: return emptyList()
        @Suppress("UNCHECKED_CAST")
        (method.invoke(instance) as? Set<String>).orEmpty().sorted()
    }.getOrDefault(emptyList())
}

private fun reflectField(instance: Any?, name: String): Any? {
    if (instance == null) return null
    return runCatching {
        val field = instance.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.get(instance)
    }.getOrNull()
}
