package cc.bkhk.monoceros.impl.mechanic.visual

import cc.bkhk.monoceros.api.mechanic.visual.*
import cc.bkhk.monoceros.impl.util.DiagnosticLogger
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import taboolib.common.LifeCycle
import taboolib.common.platform.Awake
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.submit
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class DefaultVisualService : VisualService {
    private val bossBars = ConcurrentHashMap<String, Pair<ManagedBossBar, BossBar>>()
    private val messageQueues = ConcurrentHashMap<UUID, ConcurrentLinkedQueue<QueuedMessage>>()
    @Volatile private var tickRunning = false

    override fun createBossBar(id: String, title: String, color: BarColor, style: BarStyle): ManagedBossBar {
        val managed = ManagedBossBar(id, title, 1.0, color, style)
        val bar = Bukkit.createBossBar(title, color, style)
        bossBars[id] = managed to bar
        return managed
    }
    override fun getBossBar(id: String): ManagedBossBar? = bossBars[id]?.first
    override fun removeBossBar(id: String) { bossBars.remove(id)?.second?.removeAll() }
    override fun showBossBar(id: String, playerId: UUID) {
        val player = Bukkit.getPlayer(playerId) ?: return
        bossBars[id]?.second?.addPlayer(player)
    }
    override fun hideBossBar(id: String, playerId: UUID) {
        val player = Bukkit.getPlayer(playerId) ?: return
        bossBars[id]?.second?.removePlayer(player)
    }
    override fun updateBossBar(id: String, title: String?, progress: Double?, color: BarColor?) {
        val (managed, bar) = bossBars[id] ?: return
        title?.let { managed.title = it; bar.setTitle(it) }
        progress?.let { managed.progress = it; bar.progress = it.coerceIn(0.0, 1.0) }
        color?.let { managed.color = it; bar.color = it }
    }
    override fun sendActionBar(playerId: UUID, message: String, durationTicks: Long) {
        val player = Bukkit.getPlayer(playerId) ?: return
        val colored = message.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1")
        try {
            // 通过反射调用 spigot().sendMessage(ACTION_BAR, TextComponent)
            val spigot = player.javaClass.getMethod("spigot").invoke(player)
            val chatType = Class.forName("net.md_5.bungee.api.ChatMessageType").getField("ACTION_BAR").get(null)
            val textComp = Class.forName("net.md_5.bungee.api.chat.TextComponent").getConstructor(String::class.java).newInstance(colored)
            val baseCompClass = Class.forName("net.md_5.bungee.api.chat.BaseComponent")
            val sendMethod = spigot.javaClass.getMethod("sendMessage", chatType.javaClass, baseCompClass)
            sendMethod.invoke(spigot, chatType, textComp)
        } catch (_: Exception) {
            player.sendMessage(colored)
        }
    }
    override fun sendTitle(playerId: UUID, title: String, subtitle: String, fadeIn: Int, stay: Int, fadeOut: Int) {
        val player = Bukkit.getPlayer(playerId) ?: return
        val coloredTitle = title.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1")
        val coloredSub = subtitle.replace(Regex("&([0-9a-fk-orA-FK-OR])"), "\u00a7$1")
        player.sendTitle(coloredTitle, coloredSub, fadeIn, stay, fadeOut)
    }
    override fun queueMessage(playerId: UUID, message: QueuedMessage) {
        messageQueues.computeIfAbsent(playerId) { ConcurrentLinkedQueue() }.add(message)
    }

    fun startTicking() {
        if (tickRunning) return
        tickRunning = true
        submit(period = 20L) {
            if (!tickRunning) { cancel(); return@submit }
            processQueues()
        }
    }
    fun stopTicking() {
        tickRunning = false
        bossBars.values.forEach { it.second.removeAll() }
        bossBars.clear()
        messageQueues.clear()
    }

    private fun processQueues() {
        val iterator = messageQueues.entries.iterator()
        while (iterator.hasNext()) {
            val (playerId, queue) = iterator.next()
            val msg = queue.poll() ?: continue
            when (msg.type) {
                MessageType.ACTION_BAR -> sendActionBar(playerId, msg.content, msg.durationTicks)
                MessageType.TITLE -> sendTitle(playerId, msg.content, "")
                MessageType.SUBTITLE -> sendTitle(playerId, "", msg.content)
            }
            if (queue.isEmpty()) iterator.remove()
        }
    }
}

object VisualServiceLoader {
    private lateinit var service: DefaultVisualService

    @Awake(LifeCycle.LOAD)
    fun onLoad() {
        service = DefaultVisualService()
        PlatformFactory.registerAPI<VisualService>(service)
        DiagnosticLogger.info("Visual", "视觉机制服务已注册")
    }
    @Awake(LifeCycle.ENABLE)
    fun onEnable() { service.startTicking() }
    @Awake(LifeCycle.DISABLE)
    fun onDisable() { service.stopTicking() }
}
