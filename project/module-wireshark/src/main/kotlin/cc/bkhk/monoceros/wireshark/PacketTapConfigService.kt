package cc.bkhk.monoceros.wireshark

import cc.bkhk.monoceros.api.wireshark.PacketDirection
import cc.bkhk.monoceros.api.wireshark.PacketFilterSpec
import cc.bkhk.monoceros.api.wireshark.PacketMatcherSpec
import cc.bkhk.monoceros.api.wireshark.PacketRewriteSpec
import cc.bkhk.monoceros.api.wireshark.PacketRoute
import cc.bkhk.monoceros.api.wireshark.PacketTapDefinition
import cc.bkhk.monoceros.impl.config.ConfigFileHash
import cc.bkhk.monoceros.impl.config.ConfigService
import cc.bkhk.monoceros.impl.config.ConfigServiceCallback
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import taboolib.library.configuration.ConfigurationSection
import taboolib.module.configuration.Configuration

/**
 * Packet tap 配置加载服务
 */
class PacketTapConfigService(
    private val packetService: DefaultPacketService,
) : ConfigService("wireshark") {

    private companion object {
        const val MODULE = "PacketTapService"
    }

    fun reloadAll(): Int {
        packetService.taps.clear()
        clearHashes()
        var loaded = 0
        scan(object : ConfigServiceCallback {
            override fun onCreated(fileId: String, hash: ConfigFileHash) {
                loaded += loadFile(fileId)
            }

            override fun onModified(fileId: String, hash: ConfigFileHash) {
                loaded += loadFile(fileId)
            }

            override fun onDeleted(fileId: String) = Unit
        })
        packetService.allowIntercept = packetService.taps.values.any { it.intercept }
        packetService.allowRewrite = packetService.taps.values.any { it.rewrite != null }
        DiagnosticLogger.summary(MODULE, loaded)
        return loaded
    }

    fun createWatcherCallback(): ConfigServiceCallback = object : ConfigServiceCallback {
        override fun onCreated(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到 packet tap 文件新增: $fileId")
            reloadAll()
        }

        override fun onModified(fileId: String, hash: ConfigFileHash) {
            DiagnosticLogger.info(MODULE, "检测到 packet tap 文件变更: $fileId")
            reloadAll()
        }

        override fun onDeleted(fileId: String) {
            DiagnosticLogger.info(MODULE, "检测到 packet tap 文件删除: $fileId")
            reloadAll()
        }
    }

    private fun loadFile(fileId: String): Int {
        val dir = directory()
        val file = dir.walkTopDown().find { target ->
            target.isFile && !target.name.startsWith("#") && target.extension in setOf("yml", "yaml") &&
                target.relativeTo(dir).path.replace('\\', '/').substringBeforeLast('.').replace('/', '.') == fileId
        } ?: return 0

        var count = 0
        try {
            val config = Configuration.loadFromFile(file)
            if (config.contains("direction")) {
                parseDefinition(config, fileId)?.let {
                    packetService.register(it)
                    count++
                }
                return count
            }
            for (key in config.getKeys(false)) {
                val section = config.getConfigurationSection(key) ?: continue
                if (!section.contains("direction")) continue
                val id = section.getString("id") ?: "$fileId.$key"
                parseDefinition(section, id)?.let {
                    packetService.register(it)
                    count++
                }
            }
        } catch (ex: Exception) {
            DiagnosticLogger.warn(MODULE, "packet tap 文件解析失败: ${file.path}", ex)
        }
        return count
    }

    private fun parseDefinition(section: ConfigurationSection, defaultId: String): PacketTapDefinition? {
        val id = section.getString("id") ?: defaultId
        val directions = parseDirections(section)
        val matcher = section.getConfigurationSection("matcher")?.let {
            PacketMatcherSpec(
                type = it.getString("type") ?: return@let null,
                value = it.getString("value") ?: return@let null,
            )
        }
        val filters = section.getMapList("filters").mapNotNull { map ->
            val type = map["type"]?.toString() ?: return@mapNotNull null
            val value = map["value"]?.toString() ?: return@mapNotNull null
            PacketFilterSpec(type = type, value = value)
        }
        val rewrite = section.getConfigurationSection("rewrite")?.let { rewriteSection ->
            val type = rewriteSection.getString("type") ?: return@let null
            val config = rewriteSection.getKeys(false)
                .filter { it != "type" }
                .associateWith { rewriteSection.get(it) }
            PacketRewriteSpec(type = type, config = config)
        }
        val route = parseRoute(section)
        return PacketTapDefinition(
            id = id,
            direction = directions,
            matcher = matcher,
            filters = filters,
            tracking = section.getBoolean("tracking", false),
            parse = section.getBoolean("parse", false),
            intercept = section.getBoolean("intercept", false),
            rewrite = rewrite,
            route = route,
        )
    }

    private fun parseDirections(section: ConfigurationSection): Set<PacketDirection> {
        val raw = when {
            section.isList("direction") -> section.getStringList("direction")
            else -> listOf(section.getString("direction") ?: "send")
        }
        return raw.mapNotNull {
            runCatching { PacketDirection.valueOf(it.uppercase()) }.getOrNull()
        }.toSet().ifEmpty { setOf(PacketDirection.SEND) }
    }

    private fun parseRoute(section: ConfigurationSection): PacketRoute? {
        val route = section.getConfigurationSection("route") ?: return null
        val type = route.getString("type") ?: "script"
        val value = route.getString("value") ?: return null
        return when (type.lowercase()) {
            "script" -> PacketRoute.Script(value)
            "action", "workflow" -> PacketRoute.ActionWorkflow(value)
            "handler" -> PacketRoute.Handler(value)
            else -> null
        }
    }
}
